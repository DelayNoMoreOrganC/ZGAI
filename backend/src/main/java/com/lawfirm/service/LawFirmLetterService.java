package com.lawfirm.service;

import com.lawfirm.dto.*;
import com.lawfirm.entity.*;
import com.lawfirm.event.SealApprovalDecisionEvent;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.exception.ResourceNotFoundException;
import com.lawfirm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LawFirmLetterService {
    private static final Set<String> EDITABLE_STATUSES = Set.of("DRAFT", "REJECTED");

    private final LawFirmLetterRepository letterRepository;
    private final CaseRepository caseRepository;
    private final ClientRepository clientRepository;
    private final PartyRepository partyRepository;
    private final CaseMemberRepository caseMemberRepository;
    private final UserRepository userRepository;
    private final UserPermissionService userPermissionService;
    private final CaseService caseService;
    private final ApprovalService approvalService;
    private final SealAttachmentService sealAttachmentService;
    private final LawFirmLetterDocumentService documentService;
    private final LawFirmLetterSequenceService sequenceService;
    private final CaseDocumentService caseDocumentService;
    private final CaseTimelineService caseTimelineService;

    @Transactional(readOnly = true)
    public List<LawFirmLetterDTO> list(Long caseId, Long userId) {
        caseService.assertCaseVisible(caseId, userId);
        return letterRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId).stream()
                .map(item -> toDTO(item, userId)).collect(Collectors.toList());
    }

    @Transactional
    public LawFirmLetterDTO create(Long caseId, Long userId) {
        caseService.assertCaseEditable(caseId, userId);
        Case caseEntity = requireApprovedCase(caseId);
        List<Party> parties = partyRepository.findByCaseIdAndDeletedFalse(caseId);
        List<User> lawyers = resolveLawyers(caseEntity);

        LawFirmLetter letter = new LawFirmLetter();
        letter.setCaseId(caseId);
        letter.setRecipient(defaultText(caseEntity.getCourt(), "请填写收函单位"));
        letter.setClientName(resolveClientName(caseEntity, parties));
        letter.setLawyerNames(formatLawyerNames(lawyers));
        letter.setLawyerContacts(formatLawyerContacts(lawyers));
        letter.setOpposingParty(resolveOpposingParty(parties));
        letter.setCaseReason(defaultText(caseEntity.getCaseReason(), "相关纠纷"));
        letter.setLetterTypeCode(letterTypeCode(caseEntity.getCaseType()));
        letter.setClosingText("特此函告！");
        letter.setIssueDate(LocalDate.now());
        letter.setCreatedBy(userId);
        letter.setUpdatedBy(userId);
        return toDTO(letterRepository.save(letter), userId);
    }

    @Transactional(readOnly = true)
    public LawFirmLetterDTO get(Long id, Long userId) {
        LawFirmLetter letter = requireLetter(id);
        caseService.assertCaseVisible(letter.getCaseId(), userId);
        return toDTO(letter, userId);
    }

    @Transactional
    public LawFirmLetterDTO update(Long id, LawFirmLetterUpdateRequest request, Long userId) {
        LawFirmLetter letter = requireLetter(id);
        caseService.assertCaseEditable(letter.getCaseId(), userId);
        assertEditable(letter);
        if (request.getLockVersion() != null && !Objects.equals(request.getLockVersion(), letter.getLockVersion())) {
            throw new InvalidParameterException("lockVersion", "所函已被其他人员修改，请刷新后重试");
        }
        letter.setRecipient(request.getRecipient().trim());
        letter.setClientName(request.getClientName().trim());
        letter.setLawyerNames(request.getLawyerNames().trim());
        letter.setOpposingParty(request.getOpposingParty().trim());
        letter.setCaseReason(request.getCaseReason().trim());
        letter.setLetterTypeCode(request.getLetterTypeCode().trim());
        letter.setLawyerContacts(request.getLawyerContacts().trim());
        letter.setClosingText(request.getClosingText().trim());
        letter.setIssueDate(request.getIssueDate() == null ? LocalDate.now() : request.getIssueDate());
        letter.setUpdatedBy(userId);
        letter.setRejectedReason(null);
        return toDTO(letterRepository.save(letter), userId);
    }

    @Transactional
    public void cancel(Long id, Long userId) {
        LawFirmLetter letter = requireLetter(id);
        caseService.assertCaseEditable(letter.getCaseId(), userId);
        assertEditable(letter);
        String displayNumber = StringUtils.hasText(letter.getLetterNumber())
                ? letter.getLetterNumber() : "未编号草稿 #" + letter.getId();
        letter.setDeleted(true);
        letter.setUpdatedBy(userId);
        letterRepository.save(letter);
        caseTimelineService.createSystemTimeline(letter.getCaseId(), "LAW_FIRM_LETTER_CANCELLED",
                "取消律所所函草稿：" + displayNumber, userId);
    }

    @Transactional(readOnly = true)
    public GeneratedLetterFile download(Long id, Long userId) {
        LawFirmLetter letter = requireLetter(id);
        caseService.assertCaseVisible(letter.getCaseId(), userId);
        byte[] content = documentService.generate(letter);
        return new GeneratedLetterFile(fileName(letter), content);
    }

    @Transactional
    public LawFirmLetterDTO submit(Long id, Long userId) {
        LawFirmLetter letter = requireLetter(id);
        caseService.assertCaseEditable(letter.getCaseId(), userId);
        assertEditable(letter);
        requireApprovedCase(letter.getCaseId());
        byte[] draft = documentService.generate(letter);

        ApprovalCreateRequest request = new ApprovalCreateRequest();
        request.setApprovalType(ApprovalService.TYPE_SEAL);
        request.setTitle("律所所函用印申请 - " + caseRepository.findById(letter.getCaseId())
                .map(Case::getCaseName).orElse("案件"));
        request.setContent(buildApprovalContent(letter));
        request.setCaseId(letter.getCaseId());
        validateRequiredFields(letter);
        User approver = userPermissionService.findFirstActiveUserByPermission("SEAL_APPROVE", "ADMINISTRATIVE")
                .orElseThrow(() -> new InvalidParameterException("未找到具有公章用印审批权限的行政账号，请先配置 SEAL_APPROVE 权限"));
        request.setCurrentApproverId(approver.getId());
        ApprovalDTO approval = approvalService.createSealApproval(request, userId);
        sealAttachmentService.attachGenerated(approval.getId(), draft, fileName(letter), userId);

        letter.setApprovalId(approval.getId());
        letter.setStatus("PENDING_SEAL");
        letter.setDraftSha256(sha256(draft));
        letter.setSubmittedAt(LocalDateTime.now());
        letter.setUpdatedBy(userId);
        return toDTO(letterRepository.save(letter), userId);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleSealDecision(SealApprovalDecisionEvent event) {
        LawFirmLetter letter = letterRepository.findByApprovalIdAndDeletedFalse(event.getApprovalId()).orElse(null);
        if (letter == null) return;
        if ("REJECTED".equals(event.getStatus()) || "WITHDRAWN".equals(event.getStatus())) {
            letter.setStatus("WITHDRAWN".equals(event.getStatus()) ? "DRAFT" : "REJECTED");
            letter.setRejectedReason("REJECTED".equals(event.getStatus()) ? event.getComments() : null);
            letter.setUpdatedBy(event.getOperatorId());
            letterRepository.save(letter);
            return;
        }
        if (!"APPROVED".equals(event.getStatus())) return;

        int year = event.getDecidedAt().getYear();
        int serial = sequenceService.allocate(year, letter.getLetterTypeCode(),
                event.getInitialLetterSerial(), event.getOperatorId());
        letter.setIssueDate(event.getDecidedAt().toLocalDate());
        letter.setSerialNo(serial);
        letter.setLetterNumber(String.format("(%d)粤至高%s函字第%d号", year, letter.getLetterTypeCode(), serial));
        byte[] finalContent = documentService.generate(letter);
        String finalName = fileName(letter);
        try {
            CaseDocumentDTO document = caseDocumentService.uploadDocument(
                    letter.getCaseId(), new GeneratedMultipartFile(finalName, finalContent),
                    "LAW_FIRM_LETTER", "03_法律文书", event.getOperatorId(), null, sha256(finalContent));
            sealAttachmentService.replaceGenerated(event.getApprovalId(), finalContent, finalName);
            letter.setFinalDocumentId(document.getId());
        } catch (IOException e) {
            throw new IllegalStateException("正式所函写入案件文件库失败", e);
        }
        letter.setFinalSha256(sha256(finalContent));
        letter.setStatus("APPROVED");
        letter.setApprovedAt(event.getDecidedAt());
        letter.setUpdatedBy(event.getOperatorId());
        letterRepository.save(letter);
        caseTimelineService.createSystemTimeline(letter.getCaseId(), "LAW_FIRM_LETTER_ISSUED",
                "完成律所所函用印审批并编号：" + letter.getLetterNumber(), event.getOperatorId());
    }

    @Transactional(readOnly = true)
    public Optional<LawFirmLetter> findByApprovalId(Long approvalId) {
        return letterRepository.findByApprovalIdAndDeletedFalse(approvalId);
    }

    @Transactional(readOnly = true)
    public boolean requiresInitialNumber(LawFirmLetter letter) {
        int year = letter.getIssueDate() == null ? LocalDate.now().getYear() : letter.getIssueDate().getYear();
        return sequenceService.requiresInitialNumber(year, letter.getLetterTypeCode());
    }

    private Case requireApprovedCase(Long caseId) {
        Case caseEntity = caseRepository.findById(caseId)
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("案件", caseId));
        if (caseEntity.getFilingDate() == null || !Set.of("ACTIVE", "CLOSED").contains(caseEntity.getStatus())) {
            throw new InvalidParameterException("caseId", "只有立案审批通过后的案件可以生成律所所函");
        }
        return caseEntity;
    }

    private LawFirmLetter requireLetter(Long id) {
        return letterRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("律所所函", id));
    }

    private void assertEditable(LawFirmLetter letter) {
        if (!EDITABLE_STATUSES.contains(letter.getStatus())) {
            throw new InvalidParameterException("status", "待审批或已批准的所函不能修改");
        }
    }

    private String resolveClientName(Case caseEntity, List<Party> parties) {
        if (caseEntity.getClientId() != null) {
            String value = clientRepository.findById(caseEntity.getClientId())
                    .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                    .map(Client::getClientName).orElse(null);
            if (StringUtils.hasText(value)) return value;
        }
        if (StringUtils.hasText(caseEntity.getConsultantUnitName())) return caseEntity.getConsultantUnitName();
        String partyNames = parties.stream().filter(item -> Boolean.TRUE.equals(item.getIsClient()))
                .map(Party::getName).filter(StringUtils::hasText).distinct()
                .collect(Collectors.joining("、"));
        return defaultText(partyNames, "请填写委托客户");
    }

    private String resolveOpposingParty(List<Party> parties) {
        String names = parties.stream().filter(item -> !Boolean.TRUE.equals(item.getIsClient()))
                .map(Party::getName).filter(StringUtils::hasText).distinct()
                .collect(Collectors.joining("、"));
        return defaultText(names, "请填写相对方");
    }

    private List<User> resolveLawyers(Case caseEntity) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        ids.add(caseEntity.getOwnerId());
        caseMemberRepository.findByCaseIdAndDeletedFalse(caseEntity.getId()).stream()
                .map(CaseMember::getUserId).forEach(ids::add);
        Map<Long, User> users = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, item -> item));
        return ids.stream().map(users::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private String formatLawyerNames(List<User> users) {
        return users.stream().map(user -> user.getRealName() + professionalTitle(user.getPosition()))
                .collect(Collectors.joining("、"));
    }

    private String formatLawyerContacts(List<User> users) {
        String contacts = users.stream().filter(user -> StringUtils.hasText(user.getPhone()))
                .map(user -> user.getRealName() + "：" + user.getPhone())
                .collect(Collectors.joining("    "));
        return defaultText(contacts, "请填写办案人电话");
    }

    private void validateRequiredFields(LawFirmLetter letter) {
        Map<String, String> required = new LinkedHashMap<>();
        required.put("收函单位", letter.getRecipient());
        required.put("委托客户", letter.getClientName());
        required.put("办案人", letter.getLawyerNames());
        required.put("相对方", letter.getOpposingParty());
        required.put("案由", letter.getCaseReason());
        required.put("办案人电话", letter.getLawyerContacts());
        List<String> missing = required.entrySet().stream()
                .filter(entry -> !StringUtils.hasText(entry.getValue()) || entry.getValue().startsWith("请填写"))
                .map(Map.Entry::getKey).collect(Collectors.toList());
        if (!missing.isEmpty()) {
            throw new InvalidParameterException("所函信息尚未完整，请补充：" + String.join("、", missing));
        }
    }

    private String professionalTitle(String position) {
        if (position != null && position.contains("实习")) return "实习律师";
        if (position != null && position.contains("助理")) return "助理";
        return "律师";
    }

    private String letterTypeCode(String caseType) {
        if (caseType == null) return "案";
        switch (caseType) {
            case "CIVIL": return "民";
            case "CRIMINAL": return "刑";
            case "ADMINISTRATIVE": return "行";
            case "ARBITRATION": return "仲";
            case "NON_LITIGATION": return "非";
            case "CONSULTANT": return "顾";
            default: return "案";
        }
    }

    private String buildApprovalContent(LawFirmLetter letter) {
        return "申请对系统生成的律所所函加盖公章。\n"
                + "收函单位：" + letter.getRecipient() + "\n"
                + "委托客户：" + letter.getClientName() + "\n"
                + "办案人：" + letter.getLawyerNames() + "\n"
                + "相对方：" + letter.getOpposingParty() + "\n"
                + "案由：" + letter.getCaseReason() + "\n"
                + "编号：行政审批通过后自动生成。";
    }

    private LawFirmLetterDTO toDTO(LawFirmLetter letter, Long userId) {
        LawFirmLetterDTO dto = new LawFirmLetterDTO();
        dto.setId(letter.getId()); dto.setCaseId(letter.getCaseId());
        caseRepository.findById(letter.getCaseId()).ifPresent(item -> dto.setCaseName(item.getCaseName()));
        dto.setRecipient(letter.getRecipient()); dto.setClientName(letter.getClientName());
        dto.setLawyerNames(letter.getLawyerNames()); dto.setOpposingParty(letter.getOpposingParty());
        dto.setCaseReason(letter.getCaseReason()); dto.setLetterTypeCode(letter.getLetterTypeCode());
        dto.setLawyerContacts(letter.getLawyerContacts()); dto.setClosingText(letter.getClosingText());
        dto.setIssueDate(letter.getIssueDate()); dto.setSerialNo(letter.getSerialNo());
        dto.setLetterNumber(letter.getLetterNumber()); dto.setDisplayNumber(documentService.displayNumber(letter));
        dto.setStatus(letter.getStatus()); dto.setStatusDesc(statusDesc(letter.getStatus()));
        dto.setApprovalId(letter.getApprovalId()); dto.setFinalDocumentId(letter.getFinalDocumentId());
        dto.setSubmittedAt(letter.getSubmittedAt()); dto.setApprovedAt(letter.getApprovedAt());
        dto.setRejectedReason(letter.getRejectedReason()); dto.setLockVersion(letter.getLockVersion());
        dto.setEditable(EDITABLE_STATUSES.contains(letter.getStatus()) && caseService.canEditCase(letter.getCaseId(), userId));
        dto.setDownloadable(true);
        return dto;
    }

    private String statusDesc(String status) {
        if ("DRAFT".equals(status)) return "草稿";
        if ("PENDING_SEAL".equals(status)) return "用印审批中";
        if ("APPROVED".equals(status)) return "已批准并编号";
        if ("REJECTED".equals(status)) return "已驳回，可修改";
        return status;
    }

    private String fileName(LawFirmLetter letter) {
        String number = StringUtils.hasText(letter.getLetterNumber()) ? letter.getLetterNumber() : "待编号";
        return safeName("律所所函_" + number + ".docx");
    }

    private String safeName(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder output = new StringBuilder();
            for (byte value : digest) output.append(String.format("%02x", value));
            return output.toString();
        } catch (Exception e) {
            throw new IllegalStateException("所函哈希计算失败", e);
        }
    }

    public static class GeneratedLetterFile {
        private final String fileName;
        private final byte[] content;
        public GeneratedLetterFile(String fileName, byte[] content) { this.fileName = fileName; this.content = content; }
        public String getFileName() { return fileName; }
        public byte[] getContent() { return content; }
    }

    private static class GeneratedMultipartFile implements MultipartFile {
        private final String fileName;
        private final byte[] content;
        private GeneratedMultipartFile(String fileName, byte[] content) { this.fileName = fileName; this.content = content; }
        public String getName() { return "file"; }
        public String getOriginalFilename() { return fileName; }
        public String getContentType() { return "application/vnd.openxmlformats-officedocument.wordprocessingml.document"; }
        public boolean isEmpty() { return content.length == 0; }
        public long getSize() { return content.length; }
        public byte[] getBytes() { return content.clone(); }
        public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        public void transferTo(File dest) throws IOException { try (OutputStream out = new FileOutputStream(dest)) { out.write(content); } }
        public void transferTo(Path dest) throws IOException { java.nio.file.Files.write(dest, content); }
    }
}
