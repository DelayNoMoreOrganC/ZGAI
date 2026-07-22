package com.lawfirm.service;

import com.lawfirm.dto.*;
import com.lawfirm.entity.*;
import com.lawfirm.enums.ApprovalStatus;
import com.lawfirm.repository.*;
import com.lawfirm.util.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 审批服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final ApprovalFlowRepository approvalFlowRepository;
    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final CaseDocumentRepository caseDocumentRepository;
    private final NotificationService notificationService;
    private final CaseTimelineService caseTimelineService;
    private final CaseFileLibraryService caseFileLibraryService;

    /**
     * 审批类型常量
     */
    public static final String TYPE_SEAL = "SEAL";  // 用印申请
    public static final String TYPE_REIMBURSEMENT = "REIMBURSEMENT";  // 费用报销
    public static final String TYPE_INVOICE = "INVOICE";  // 开票申请
    public static final String TYPE_LEAVE = "LEAVE";  // 请假出差
    public static final String TYPE_PURCHASE = "PURCHASE";  // 采购申请
    public static final String TYPE_LICENSE = "LICENSE";  // 证照借用
    public static final String TYPE_CASE_FILING = "CASE_FILING";  // 立案审批
    public static final String TYPE_CASE_FILING_DIRECTOR = "CASE_FILING_DIRECTOR";  // 免费代理主任终审

    /**
     * 创建审批
     */
    @Transactional
    public ApprovalDTO createApproval(ApprovalCreateRequest request, Long currentUserId) {
        Approval approval = new Approval();
        BeanUtils.copyProperties(request, approval);
        approval.setApplicantId(currentUserId);
        approval.setStatus(ApprovalStatus.PENDING.getCode());
        approval.setApplyTime(LocalDateTime.now());

        // 如果关联案件，验证案件是否存在
        if (request.getCaseId() != null) {
            Case caseEntity = caseRepository.findById(request.getCaseId())
                    .orElseThrow(() -> new RuntimeException("案件不存在"));
            // 可以在这里添加更多业务逻辑
        }

        approval = approvalRepository.save(approval);

        // 记录流程
        recordFlow(approval.getId(), currentUserId, "SUBMIT", "提交审批");
        sendApprovalPendingNotification(approval);

        return toDTO(approval);
    }

    /**
     * 同意审批
     */
    @Transactional
    public void approveApproval(Long approvalId, String comments, Long approverId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("审批人不存在"));

        // 验证审批人
        if (!canOperateApprovalAsApprover(approval, approver)) {
            throw new RuntimeException("您不是当前审批人");
        }

        // 验证状态
        if (!ApprovalStatus.PENDING.getCode().equals(approval.getStatus())) {
            throw new RuntimeException("审批单状态不正确");
        }

        if (isCaseFilingApproval(approval) && !hasText(comments)) {
            throw new RuntimeException("立案审批意见不能为空");
        }

        // 更新审批单状态
        approval.setStatus(ApprovalStatus.APPROVED.getCode());
        approval.setApprovedTime(LocalDateTime.now());
        approval.setApprovalNotes(comments);

        approvalRepository.save(approval);

        // 记录流程
        recordFlow(approvalId, approverId, "APPROVE", comments);

        if (approval.getCaseId() != null) {
            if (TYPE_CASE_FILING.equals(approval.getApprovalType())) {
                approveCaseFilingAdminStep(approval, comments, approverId);
            } else if (TYPE_CASE_FILING_DIRECTOR.equals(approval.getApprovalType())) {
                approveCaseFilingFinal(approval, comments, approverId);
            }
        } else {
            notifyApplicant(
                    approval,
                    "审批申请已通过",
                    String.format("您发起的审批「%s」已由 %s 处理通过。", approval.getTitle(), getUserName(approverId))
            );
        }
    }

    /**
     * 驳回审批
     */
    @Transactional
    public void rejectApproval(Long approvalId, String comments, Long approverId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("审批人不存在"));

        // 验证审批人
        if (!canOperateApprovalAsApprover(approval, approver)) {
            throw new RuntimeException("您不是当前审批人");
        }

        // 验证状态
        if (!ApprovalStatus.PENDING.getCode().equals(approval.getStatus())) {
            throw new RuntimeException("审批单状态不正确");
        }

        if (!hasText(comments)) {
            throw new RuntimeException("驳回理由不能为空");
        }

        // 更新审批单状态
        approval.setStatus(ApprovalStatus.REJECTED.getCode());
        approval.setApprovedTime(LocalDateTime.now());
        approval.setApprovalNotes(comments);

        approvalRepository.save(approval);

        // 记录流程
        recordFlow(approvalId, approverId, "REJECT", comments);
        notifyApplicant(
                approval,
                isCaseFilingApproval(approval) ? "立案申请已驳回" : "审批申请已驳回",
                String.format("您发起的审批「%s」已被驳回。原因：%s", approval.getTitle(), comments == null ? "" : comments)
        );

        if ((TYPE_CASE_FILING.equals(approval.getApprovalType()) || TYPE_CASE_FILING_DIRECTOR.equals(approval.getApprovalType()))
                && approval.getCaseId() != null) {
            rejectCaseFiling(approval, comments, approverId);
        }
    }

    /**
     * 转审
     */
    @Transactional
    public void transferApproval(Long approvalId, Long newApproverId, String comments, Long currentApproverId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));

        User currentApprover = userRepository.findById(currentApproverId)
                .orElseThrow(() -> new RuntimeException("审批人不存在"));

        // 验证当前审批人
        if (!canOperateApprovalAsApprover(approval, currentApprover)) {
            throw new RuntimeException("您不是当前审批人");
        }

        // 验证新审批人
        if (!userRepository.existsById(newApproverId)) {
            throw new RuntimeException("新审批人不存在");
        }

        // 验证状态
        if (!ApprovalStatus.PENDING.getCode().equals(approval.getStatus())) {
            throw new RuntimeException("审批单状态不正确");
        }

        Long oldApproverId = approval.getCurrentApproverId();

        // 更新审批单
        approval.setCurrentApproverId(newApproverId);
        approval.setStatus(ApprovalStatus.TRANSFERRED.getCode());

        approvalRepository.save(approval);

        // 记录流程
        recordFlow(approvalId, currentApproverId, "TRANSFER",
                "转给" + getUserName(newApproverId) + "，备注：" + comments);
        sendApprovalPendingNotification(approval);

        // 重置为待审批状态
        approval.setStatus(ApprovalStatus.PENDING.getCode());
        approvalRepository.save(approval);
    }

    /**
     * 撤回审批
     */
    @Transactional
    public void withdrawApproval(Long approvalId, Long applicantId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));

        // 验证申请人
        if (!approval.getApplicantId().equals(applicantId)) {
            throw new RuntimeException("您不是申请人，无法撤回");
        }

        // 验证状态
        if (!ApprovalStatus.PENDING.getCode().equals(approval.getStatus())) {
            throw new RuntimeException("只能撤回待审批的单据");
        }

        // 更新状态
        approval.setStatus(ApprovalStatus.WITHDRAWN.getCode());
        approvalRepository.save(approval);

        // 记录流程
        recordFlow(approvalId, applicantId, "WITHDRAW", "申请人撤回");
    }

    /**
     * 催办
     */
    @Transactional
    public void urgeApproval(Long approvalId, Long applicantId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));

        // 验证申请人
        if (!approval.getApplicantId().equals(applicantId)) {
            throw new RuntimeException("您不是申请人，无法催办");
        }

        // 验证状态
        if (!ApprovalStatus.PENDING.getCode().equals(approval.getStatus())) {
            throw new RuntimeException("只能催办待审批的单据");
        }

        // 记录流程（作为催办记录）
        recordFlow(approvalId, applicantId, "URGE", "申请人催办");

        // 发送催办通知
        User applicant = userRepository.findById(applicantId).orElse(null);
        String applicantName = applicant != null ? applicant.getRealName() : "申请人";
        String title = "审批催办提醒";
        String content = String.format("审批单「%s」被 %s 催办，请及时处理。", approval.getTitle(), applicantName);

        notificationService.sendNotification(
                approval.getCurrentApproverId(),
                title,
                content,
                NotificationService.CATEGORY_APPROVAL,
                approvalId,
                "APPROVAL_URGE"
        );

        log.info("审批单 {} 已催办，通知审批人：{}", approvalId, approval.getCurrentApproverId());
    }

    /**
     * 获取审批列表
     */
    public PageResult<ApprovalDTO> getApprovalList(ApprovalQueryRequest request, Long currentUserId) {
        Pageable pageable = PageRequest.of(
                Math.max(0, request.getPage() - 1),
                request.getSize(),
                Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortField())
        );

        boolean hasAllAccess = hasAllApprovalAccess(currentUserId);
        if (!hasAllAccess && request.getApplicantId() != null && !Objects.equals(request.getApplicantId(), currentUserId)) {
            throw new RuntimeException("无权查看他人发起的审批");
        }
        if (!hasAllAccess && request.getCurrentApproverId() != null && !Objects.equals(request.getCurrentApproverId(), currentUserId)) {
            throw new RuntimeException("无权查看他人的审批待办");
        }

        Specification<Approval> spec = (root, query, cb) -> {
            List<javax.persistence.criteria.Predicate> predicates = new ArrayList<>();

            // 基础条件
            if (request.getApprovalType() != null) {
                predicates.add(cb.equal(root.get("approvalType"), request.getApprovalType()));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            if (request.getApplicantId() != null) {
                predicates.add(cb.equal(root.get("applicantId"), request.getApplicantId()));
            } else if (request.getCurrentApproverId() != null) {
                predicates.add(cb.equal(root.get("currentApproverId"), request.getCurrentApproverId()));
            } else if (!hasAllAccess) {
                // 默认：查看自己相关的
                javax.persistence.criteria.Predicate applicantCondition =
                        cb.equal(root.get("applicantId"), currentUserId);
                javax.persistence.criteria.Predicate approverCondition =
                        cb.equal(root.get("currentApproverId"), currentUserId);
                predicates.add(cb.or(applicantCondition, approverCondition));
            }

            if (request.getCaseId() != null) {
                predicates.add(cb.equal(root.get("caseId"), request.getCaseId()));
            }

            if (request.getKeyword() != null) {
                String keyword = "%" + request.getKeyword() + "%";
                javax.persistence.criteria.Predicate titleCondition =
                        cb.like(root.get("title"), keyword);
                javax.persistence.criteria.Predicate contentCondition =
                        cb.like(root.get("content"), keyword);
                predicates.add(cb.or(titleCondition, contentCondition));
            }

            // 排除已删除
            predicates.add(cb.equal(root.get("deleted"), false));

            return cb.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
        };

        Page<Approval> page = approvalRepository.findAll(spec, pageable);

        List<ApprovalDTO> records = page.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        PageResult<ApprovalDTO> result = PageResult.of(
                (long) request.getPage(),
                (long) request.getSize(),
                page.getTotalElements(),
                records
        );
        result.setTotalPages((long) page.getTotalPages());
        result.setHasPrevious(page.hasPrevious());
        result.setHasNext(page.hasNext());
        return result;
    }

    /**
     * 获取审批详情
     */
    public ApprovalDTO getApprovalDetail(Long approvalId, Long currentUserId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));
        assertApprovalVisible(approval, currentUserId);
        return toDTO(approval);
    }

    /**
     * 获取审批流程记录
     */
    public List<ApprovalFlow> getApprovalFlow(Long approvalId, Long currentUserId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));
        assertApprovalVisible(approval, currentUserId);
        List<ApprovalFlow> flows = approvalFlowRepository.findByApprovalIdOrderByActionTimeAsc(approvalId);
        flows.forEach(flow -> flow.setApproverName(getUserName(flow.getApproverId())));
        return flows;
    }

    /**
     * 获取审批类型列表
     */
    public List<Map<String, String>> getApprovalTypes() {
        List<Map<String, String>> types = new ArrayList<>();
        types.add(createTypeItem(TYPE_SEAL, "用印申请"));
        types.add(createTypeItem(TYPE_REIMBURSEMENT, "费用报销"));
        types.add(createTypeItem(TYPE_INVOICE, "开票申请"));
        types.add(createTypeItem(TYPE_LEAVE, "请假出差"));
        types.add(createTypeItem(TYPE_PURCHASE, "采购申请"));
        types.add(createTypeItem(TYPE_LICENSE, "证照借用"));
        types.add(createTypeItem(TYPE_CASE_FILING, "立案审批"));
        types.add(createTypeItem(TYPE_CASE_FILING_DIRECTOR, "免费代理主任终审"));
        return types;
    }

    // 辅助方法

    private void approveCaseFilingAdminStep(Approval approval, String comments, Long approverId) {
        Case caseEntity = caseRepository.findById(approval.getCaseId())
                .orElseThrow(() -> new RuntimeException("案件不存在"));

        caseTimelineService.createSystemTimeline(
                caseEntity.getId(),
                "CASE_FILING_ADMIN_APPROVED",
                "行政管理已完成立案初审：" + (comments == null ? "" : comments)
        );

        if ("FREE".equals(caseEntity.getFeeMethod())) {
            Long directorId = findUserIdByPosition("主任");
            if (directorId == null) {
                throw new RuntimeException("未找到身份类别为“主任”的终审人，请先在用户管理中配置主任账号");
            }

            Approval directorApproval = new Approval();
            directorApproval.setApprovalType(TYPE_CASE_FILING_DIRECTOR);
            directorApproval.setTitle("免费代理主任终审：" + caseEntity.getCaseName());
            directorApproval.setContent("行政管理已完成初审，请主任对免费代理立案申请进行终审。"
                    + "\n案件名称：" + caseEntity.getCaseName()
                    + "\n免费理由：" + (caseEntity.getFreeReason() == null ? "" : caseEntity.getFreeReason()));
            directorApproval.setCaseId(caseEntity.getId());
            directorApproval.setApplicantId(approval.getApplicantId());
            directorApproval.setCurrentApproverId(directorId);
            directorApproval.setStatus(ApprovalStatus.PENDING.getCode());
            directorApproval.setApplyTime(LocalDateTime.now());
            directorApproval = approvalRepository.save(directorApproval);
            recordFlow(directorApproval.getId(), approverId, "SUBMIT", "行政管理初审通过，提交主任终审");
            sendApprovalPendingNotification(directorApproval);
            notifyApplicant(
                    approval,
                    "立案行政初审已通过",
                    String.format("您发起的立案审批「%s」已通过行政初审，现已提交主任终审。", approval.getTitle())
            );

            caseTimelineService.createSystemTimeline(
                    caseEntity.getId(),
                    "CASE_FILING_DIRECTOR_REVIEW",
                    "免费代理案件已提交主任终审：" + getUserName(directorId)
            );
            return;
        }

        approveCaseFilingFinal(approval, comments, approverId);
    }

    private Long findUserIdByPosition(String position) {
        return userRepository.findAll().stream()
                .filter(user -> !Boolean.TRUE.equals(user.getDeleted()))
                .filter(user -> user.getStatus() == null || user.getStatus() == 1)
                .filter(user -> matchesPosition(user.getPosition(), position))
                .sorted(Comparator.comparing((User user) -> user.getDepartmentId() == null))
                .map(User::getId)
                .findFirst()
                .orElse(null);
    }

    private boolean matchesPosition(String actual, String expected) {
        if (actual == null || actual.trim().isEmpty() || expected == null || expected.trim().isEmpty()) {
            return false;
        }
        if ("行政管理".equals(expected)) {
            return actual.startsWith("行政管理");
        }
        return expected.equals(actual);
    }

    private void approveCaseFilingFinal(Approval approval, String comments, Long approverId) {
        Case caseEntity = caseRepository.findById(approval.getCaseId())
                .orElseThrow(() -> new RuntimeException("案件不存在"));

        ensureOfficialCaseNumber(caseEntity);
        String folderPath = caseFileLibraryService.ensureCaseFolder(caseEntity).toString();
        createConflictCheckReport(caseEntity, approval, comments, approverId, folderPath);

        caseEntity.setStatus("ACTIVE");
        if (caseEntity.getFilingDate() == null) {
            caseEntity.setFilingDate(java.time.LocalDate.now());
        }
        caseRepository.save(caseEntity);

        caseTimelineService.createSystemTimeline(
                caseEntity.getId(),
                "CASE_FILING_APPROVED",
                "立案审批已通过，案卷文件夹已建立：" + folderPath
        );
        notifyApplicant(
                approval,
                getCaseFilingApprovedTitle(approval),
                String.format("您发起的立案审批「%s」已通过，案件档案已建立。", approval.getTitle())
        );
    }

    private void sendApprovalPendingNotification(Approval approval) {
        if (approval.getCurrentApproverId() == null) {
            log.warn("审批通知未发送：currentApproverId为空，approvalId={}", approval.getId());
            return;
        }
        String title = isCaseFilingApproval(approval) ? "待处理立案审批" : "待处理审批";
        String content = String.format("您有新的%s「%s」需要处理，申请人：%s。",
                getApprovalTypeName(approval.getApprovalType()),
                approval.getTitle(),
                getUserName(approval.getApplicantId()));
        notificationService.sendNotification(
                approval.getCurrentApproverId(),
                title,
                content,
                NotificationService.CATEGORY_APPROVAL,
                approval.getId(),
                "APPROVAL_PENDING"
        );
    }

    private void notifyApplicant(Approval approval, String title, String content) {
        if (approval.getApplicantId() == null) {
            return;
        }
        notificationService.sendNotification(
                approval.getApplicantId(),
                title,
                content,
                NotificationService.CATEGORY_APPROVAL,
                approval.getId(),
                "APPROVAL_RESULT"
        );
    }

    private boolean isCaseFilingApproval(Approval approval) {
        return approval != null
                && (TYPE_CASE_FILING.equals(approval.getApprovalType())
                || TYPE_CASE_FILING_DIRECTOR.equals(approval.getApprovalType()));
    }

    private String getCaseFilingApprovedTitle(Approval approval) {
        if (TYPE_CASE_FILING_DIRECTOR.equals(approval.getApprovalType())) {
            return "主任终审已通过";
        }
        if (TYPE_CASE_FILING.equals(approval.getApprovalType())) {
            return "立案审批已通过";
        }
        return "审批申请已通过";
    }

    private String getApprovalTypeName(String approvalType) {
        if (approvalType == null) {
            return "审批";
        }
        switch (approvalType) {
            case TYPE_CASE_FILING:
                return "立案审批";
            case TYPE_CASE_FILING_DIRECTOR:
                return "主任终审";
            case TYPE_SEAL:
                return "用印申请";
            case TYPE_REIMBURSEMENT:
                return "费用报销";
            case TYPE_INVOICE:
                return "开票申请";
            case TYPE_LEAVE:
                return "请假出差";
            case TYPE_PURCHASE:
                return "采购申请";
            case TYPE_LICENSE:
                return "证照借用";
            default:
                return "审批";
        }
    }

    private void ensureOfficialCaseNumber(Case caseEntity) {
        if (caseEntity.getCaseNumber() != null && caseEntity.getCaseNumber().matches("\\[\\d{4}\\]粤至高.+第\\d{3}号")) {
            return;
        }
        String year = String.valueOf(java.time.Year.now().getValue());
        String type = getOfficialCaseTypeChar(caseEntity.getCaseType());
        long count = caseRepository.countByCaseTypeAndDeletedFalse(caseEntity.getCaseType()) + 1;
        caseEntity.setCaseNumber(String.format("[%s]粤至高%s字第%03d号", year, type, count));
    }

    private String getOfficialCaseTypeChar(String caseType) {
        switch (caseType) {
            case "CIVIL": return "民";
            case "CRIMINAL": return "刑";
            case "ADMINISTRATIVE": return "行";
            case "NON_LITIGATION": return "非";
            case "CONSULTANT": return "顾";
            default: return "案";
        }
    }

    private void rejectCaseFiling(Approval approval, String comments, Long approverId) {
        Case caseEntity = caseRepository.findById(approval.getCaseId())
                .orElseThrow(() -> new RuntimeException("案件不存在"));
        caseEntity.setStatus("FILING_REJECTED");
        caseRepository.save(caseEntity);

        caseTimelineService.createSystemTimeline(
                caseEntity.getId(),
                "CASE_FILING_REJECTED",
                getUserName(approverId) + "驳回立案申请：" + comments.trim()
        );
    }

    private void createConflictCheckReport(Case caseEntity, Approval approval, String comments, Long approverId, String folderPath) {
        String fileName = "利冲审查报告_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + ".txt";
        String folder = "01_立案材料";
        Path reportPath = Paths.get(folderPath, folder, fileName);
        String report = "利冲审查报告\n"
                + "案件名称：" + caseEntity.getCaseName() + "\n"
                + "案号：" + caseEntity.getCaseNumber() + "\n"
                + "案由：" + (caseEntity.getCaseReason() == null ? "" : caseEntity.getCaseReason()) + "\n"
                + "审查人：" + getUserName(approverId) + "\n"
                + "审查时间：" + LocalDateTime.now() + "\n"
                + "审查结论：" + (comments == null || comments.trim().isEmpty() ? "通过" : comments.trim()) + "\n";

        try {
            Files.createDirectories(reportPath.getParent());
            Files.write(reportPath, report.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("生成利冲审查报告失败: " + e.getMessage(), e);
        }

        CaseDocument document = new CaseDocument();
        document.setCaseId(caseEntity.getId());
        document.setDocumentName(fileName);
        document.setOriginalFileName(fileName);
        document.setDocumentType("CONFLICT_CHECK");
        document.setFilePath(reportPath.toString());
        document.setFileSize(reportPath.toFile().length());
        document.setMimeType("text/plain");
        document.setFolderPath(folder);
        document.setFolderId(caseFileLibraryService.findCaseFolder(caseEntity.getId(), folder).map(DocumentFolder::getId).orElse(null));
        document.setVersionNo(1);
        document.setUploadBy(approverId);
        document.setTags("立案审批,利冲审查");
        document.setKnowledgeEligible(false);
        document.setIndexStatus("FORBIDDEN");
        caseDocumentRepository.save(document);
    }

    private void recordFlow(Long approvalId, Long approverId, String action, String comments) {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setApprovalId(approvalId);
        flow.setApproverId(approverId);
        flow.setAction(action);
        flow.setComments(comments);
        flow.setActionTime(LocalDateTime.now());
        approvalFlowRepository.save(flow);
    }

    private ApprovalDTO toDTO(Approval approval) {
        ApprovalDTO dto = new ApprovalDTO();
        BeanUtils.copyProperties(approval, dto);

        dto.setApplicantName(getUserName(approval.getApplicantId()));
        dto.setCurrentApproverName(getUserName(approval.getCurrentApproverId()));
        dto.setStatusDesc(getStatusDesc(approval.getStatus()));

        if (approval.getCaseId() != null) {
            caseRepository.findById(approval.getCaseId()).ifPresent(c -> {
                dto.setCaseName(c.getCaseName());
            });
        }

        return dto;
    }

    private String getUserName(Long userId) {
        return userRepository.findById(userId)
                .map(User::getRealName)
                .orElse("未知");
    }

    private boolean canOperateApprovalAsApprover(Approval approval, User user) {
        if (approval == null || user == null) {
            return false;
        }
        return Objects.equals(approval.getCurrentApproverId(), user.getId())
                || isDevelopmentAdmin(user)
                || isDirector(user);
    }

    private void assertApprovalVisible(Approval approval, Long currentUserId) {
        if (approval == null) {
            throw new RuntimeException("审批单不存在");
        }
        if (hasAllApprovalAccess(currentUserId)
                || Objects.equals(approval.getApplicantId(), currentUserId)
                || Objects.equals(approval.getCurrentApproverId(), currentUserId)) {
            return;
        }
        throw new RuntimeException("无权查看该审批");
    }

    private boolean hasAllApprovalAccess(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        if (isDevelopmentAdmin(user)) {
            return true;
        }
        String position = user.getPosition();
        return isDirector(user) || (position != null && position.startsWith("行政管理"));
    }

    private boolean isDevelopmentAdmin(User user) {
        return user != null && "admin".equals(user.getUsername());
    }

    private boolean isDirector(User user) {
        return user != null && "主任".equals(user.getPosition());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String getStatusDesc(String status) {
        if (status == null) return null;
        switch (status) {
            case "PENDING": return "待审批";
            case "APPROVED": return "已同意";
            case "REJECTED": return "已驳回";
            case "TRANSFERRED": return "已转审";
            case "WITHDRAWN": return "已撤回";
            default: return status;
        }
    }

    private Map<String, String> createTypeItem(String code, String name) {
        Map<String, String> item = new HashMap<>();
        item.put("code", code);
        item.put("name", name);
        return item;
    }
}
