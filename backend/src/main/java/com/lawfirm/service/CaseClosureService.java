package com.lawfirm.service;

import com.lawfirm.dto.CaseClosureCreateRequest;
import com.lawfirm.dto.CaseClosureDocumentDTO;
import com.lawfirm.dto.CaseClosureRequestDTO;
import com.lawfirm.entity.*;
import com.lawfirm.enums.ApprovalStatus;
import com.lawfirm.enums.CaseStatus;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.exception.ResourceNotFoundException;
import com.lawfirm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CaseClosureService {
    public static final String APPROVAL_TYPE = "CASE_CLOSURE";

    private static final Map<String, String> CLOSURE_TYPES = orderedMap(
            "JUDGMENT", "裁判/裁决结案",
            "SETTLEMENT", "调解或和解结案",
            "WITHDRAWAL", "撤诉/撤回申请",
            "EXECUTION_COMPLETED", "执行完毕",
            "SERVICE_COMPLETED", "委托事项完成",
            "TERMINATED", "委托终止",
            "OTHER", "其他");
    private static final Map<String, String> FEE_STATUSES = orderedMap(
            "SETTLED", "费用已结清",
            "OUTSTANDING_CONFIRMED", "欠费已确认并另行跟进",
            "NO_FEE", "无收费或免费案件");
    private static final Map<String, String> DELIVERY_STATUSES = orderedMap(
            "COMPLETED", "已向客户交付/告知",
            "NOT_REQUIRED", "无需交付",
            "PENDING_CONFIRMED", "待交付事项已书面确认");

    private final CaseClosureRequestRepository closureRequestRepository;
    private final CaseClosureDocumentRepository closureDocumentRepository;
    private final CaseRepository caseRepository;
    private final CaseStageRepository caseStageRepository;
    private final CaseDocumentRepository caseDocumentRepository;
    private final ApprovalRepository approvalRepository;
    private final ApprovalFlowRepository approvalFlowRepository;
    private final UserRepository userRepository;
    private final TodoRepository todoRepository;
    private final UserPermissionService userPermissionService;
    private final ObjectProvider<CaseService> caseServiceProvider;
    private final NotificationService notificationService;
    private final CaseTimelineService caseTimelineService;

    @Transactional
    public CaseClosureRequestDTO create(Long caseId, CaseClosureCreateRequest request, Long applicantId) {
        caseServiceProvider.getObject().assertCaseEditable(caseId, applicantId);
        Case caseEntity = requireCase(caseId);
        requireActiveAtFinalStage(caseEntity);
        validateCodes(request);
        if (!closureRequestRepository.findByCaseIdAndStatusAndDeletedFalse(caseId, "PENDING").isEmpty()) {
            throw new InvalidParameterException("caseId", "该案件已有待复核的结案申请");
        }

        List<Long> requestedDocumentIds = request.getBasisDocumentIds().stream()
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<CaseDocument> documents = caseDocumentRepository.findAllById(requestedDocumentIds).stream()
                .filter(document -> caseId.equals(document.getCaseId()) && !Boolean.TRUE.equals(document.getDeleted()))
                .collect(Collectors.toList());
        if (documents.size() != requestedDocumentIds.size()) {
            throw new InvalidParameterException("basisDocumentIds", "结案依据文件不存在或不属于当前案件");
        }

        Long reviewerId = userPermissionService.findFirstActiveUserByPermission(
                        "CASE_ARCHIVE_REVIEW", Arrays.asList("CASE_FILING_ADMIN", "ADMINISTRATIVE"))
                .map(User::getId)
                .orElseThrow(() -> new InvalidParameterException(
                        "currentApproverId", "未找到具有案件归档复核权限的行政账号"));

        Approval approval = new Approval();
        approval.setApprovalType(APPROVAL_TYPE);
        approval.setTitle("案件结案申请：" + caseEntity.getCaseName());
        approval.setContent(buildApprovalContent(caseEntity, request, documents));
        approval.setCaseId(caseId);
        approval.setApplicantId(applicantId);
        approval.setCurrentApproverId(reviewerId);
        approval.setStatus(ApprovalStatus.PENDING.getCode());
        approval.setApplyTime(LocalDateTime.now());
        approval = approvalRepository.save(approval);

        Todo reviewTodo = new Todo();
        reviewTodo.setTitle("[结案复核] " + caseEntity.getCaseName());
        reviewTodo.setDescription("核对案件结果、收费、客户交付及结案依据，处理后案件才会正式结案。");
        reviewTodo.setStatus("PENDING");
        reviewTodo.setPriority("IMPORTANT");
        reviewTodo.setDueDate(LocalDateTime.now().plusDays(3));
        reviewTodo.setAssigneeId(reviewerId);
        reviewTodo.setCaseId(caseId);
        reviewTodo.setReminder(true);
        reviewTodo = todoRepository.save(reviewTodo);

        CaseClosureRequest closure = new CaseClosureRequest();
        closure.setCaseId(caseId);
        closure.setApprovalId(approval.getId());
        closure.setApplicantId(applicantId);
        closure.setReviewTodoId(reviewTodo.getId());
        closure.setClosureType(request.getClosureType());
        closure.setCaseOutcome(request.getCaseOutcome().trim());
        closure.setClosureSummary(request.getClosureSummary().trim());
        closure.setFeeStatus(request.getFeeStatus());
        closure.setClientDeliveryStatus(request.getClientDeliveryStatus());
        closure.setClientDeliveryNotes(trimToNull(request.getClientDeliveryNotes()));
        closure.setDocumentsConfirmed(true);
        closure.setStatus("PENDING");
        closure.setRequestedAt(LocalDateTime.now());
        closure = closureRequestRepository.save(closure);

        for (CaseDocument document : documents) {
            CaseClosureDocument link = new CaseClosureDocument();
            link.setClosureRequestId(closure.getId());
            link.setCaseDocumentId(document.getId());
            closureDocumentRepository.save(link);
        }

        recordFlow(approval.getId(), applicantId, "SUBMIT", "提交案件结案申请");
        notificationService.sendNotification(
                reviewerId,
                "待处理案件结案申请",
                "案件「" + caseEntity.getCaseName() + "」已提交结案复核，申请人：" + userName(applicantId) + "。",
                NotificationService.CATEGORY_APPROVAL,
                approval.getId(),
                "APPROVAL_PENDING");
        caseTimelineService.createSystemTimeline(caseId, "CASE_CLOSURE_REQUESTED",
                "提交了行政结案复核，当前审批人：" + userName(reviewerId), applicantId);
        return toDTO(closure);
    }

    @Transactional(readOnly = true)
    public CaseClosureRequestDTO getLatest(Long caseId, Long userId) {
        caseServiceProvider.getObject().assertCaseVisible(caseId, userId);
        return closureRequestRepository.findFirstByCaseIdAndDeletedFalseOrderByRequestedAtDesc(caseId)
                .map(this::toDTO).orElse(null);
    }

    @Transactional(readOnly = true)
    public CaseClosureRequestDTO getByApprovalId(Long approvalId) {
        return closureRequestRepository.findByApprovalIdAndDeletedFalse(approvalId)
                .map(this::toDTO).orElse(null);
    }

    @Transactional
    public void approve(Long approvalId, Long reviewerId, LocalDateTime reviewedAt, String comments) {
        CaseClosureRequest closure = requireByApprovalId(approvalId);
        if (!"PENDING".equals(closure.getStatus())) {
            throw new InvalidParameterException("结案申请状态不正确");
        }
        Case caseEntity = requireCase(closure.getCaseId());
        requireActiveAtFinalStage(caseEntity);

        CaseStage currentStage = caseStageRepository.findCurrentStage(caseEntity.getId())
                .orElseThrow(() -> new InvalidParameterException("caseId", "案件没有进行中的办理阶段"));
        currentStage.setStatus("COMPLETED");
        currentStage.setEndDate(reviewedAt.toLocalDate());
        caseStageRepository.save(currentStage);

        closure.setStatus("APPROVED");
        closure.setReviewedBy(reviewerId);
        closure.setReviewedAt(reviewedAt);
        closure.setReviewNotes(comments.trim());
        closureRequestRepository.save(closure);
        completeReviewTodo(closure, reviewedAt);

        caseEntity.setStatus(CaseStatus.CLOSED.getCode());
        caseEntity.setCloseStatus(closure.getClosureType());
        caseEntity.setCloseDate(reviewedAt.toLocalDate());
        caseRepository.save(caseEntity);
        caseTimelineService.createSystemTimeline(caseEntity.getId(), "CASE_CLOSED",
                "通过行政结案复核，案件已结案。结案方式：" + label(CLOSURE_TYPES, closure.getClosureType()), reviewerId);
    }

    @Transactional
    public void reject(Long approvalId, Long reviewerId, LocalDateTime reviewedAt, String comments) {
        CaseClosureRequest closure = requireByApprovalId(approvalId);
        if (!"PENDING".equals(closure.getStatus())) {
            throw new InvalidParameterException("结案申请状态不正确");
        }
        closure.setStatus("REJECTED");
        closure.setReviewedBy(reviewerId);
        closure.setReviewedAt(reviewedAt);
        closure.setReviewNotes(comments.trim());
        closureRequestRepository.save(closure);
        completeReviewTodo(closure, reviewedAt);
        caseTimelineService.createSystemTimeline(closure.getCaseId(), "CASE_CLOSURE_REJECTED",
                "驳回了结案申请：" + comments.trim(), reviewerId);
    }

    @Transactional
    public void withdraw(Long approvalId, Long applicantId) {
        CaseClosureRequest closure = requireByApprovalId(approvalId);
        if (!Objects.equals(closure.getApplicantId(), applicantId) || !"PENDING".equals(closure.getStatus())) {
            throw new InvalidParameterException("结案申请不能撤回");
        }
        closure.setStatus("WITHDRAWN");
        closureRequestRepository.save(closure);
        completeReviewTodo(closure, LocalDateTime.now());
        caseTimelineService.createSystemTimeline(closure.getCaseId(), "CASE_CLOSURE_WITHDRAWN", "撤回了结案申请", applicantId);
    }

    private void requireActiveAtFinalStage(Case caseEntity) {
        if (!CaseStatus.ACTIVE.getCode().equals(caseEntity.getStatus())) {
            throw new InvalidParameterException("caseId", "只有办理中的案件可以申请结案");
        }
        List<CaseStage> stages = caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(caseEntity.getId());
        CaseStage current = caseStageRepository.findCurrentStage(caseEntity.getId())
                .orElseThrow(() -> new InvalidParameterException("caseId", "案件没有进行中的办理阶段"));
        if (stages.isEmpty() || !Objects.equals(stages.get(stages.size() - 1).getId(), current.getId())) {
            throw new InvalidParameterException("currentStage", "案件进入最后办理阶段后才能申请结案");
        }
    }

    private void validateCodes(CaseClosureCreateRequest request) {
        requireCode(CLOSURE_TYPES, request.getClosureType(), "closureType", "结案方式");
        requireCode(FEE_STATUSES, request.getFeeStatus(), "feeStatus", "费用处理状态");
        requireCode(DELIVERY_STATUSES, request.getClientDeliveryStatus(), "clientDeliveryStatus", "客户交付状态");
        if (!"NOT_REQUIRED".equals(request.getClientDeliveryStatus())
                && !hasText(request.getClientDeliveryNotes())) {
            throw new InvalidParameterException("clientDeliveryNotes", "请填写客户交付或后续安排说明");
        }
    }

    private String buildApprovalContent(Case caseEntity, CaseClosureCreateRequest request, List<CaseDocument> documents) {
        return "请核对案件结果、收费、客户交付及结案依据后确认是否结案。"
                + "\n案件名称：" + caseEntity.getCaseName()
                + "\n案件编号：" + value(caseEntity.getCaseNumber())
                + "\n结案方式：" + label(CLOSURE_TYPES, request.getClosureType())
                + "\n案件结果：" + request.getCaseOutcome().trim()
                + "\n费用状态：" + label(FEE_STATUSES, request.getFeeStatus())
                + "\n客户交付：" + label(DELIVERY_STATUSES, request.getClientDeliveryStatus())
                + "\n结案依据：" + documents.stream().map(CaseDocument::getOriginalFileName).collect(Collectors.joining("、"))
                + "\n结案小结：" + request.getClosureSummary().trim();
    }

    private CaseClosureRequestDTO toDTO(CaseClosureRequest closure) {
        CaseClosureRequestDTO dto = new CaseClosureRequestDTO();
        dto.setId(closure.getId());
        dto.setCaseId(closure.getCaseId());
        dto.setApprovalId(closure.getApprovalId());
        dto.setApplicantId(closure.getApplicantId());
        dto.setApplicantName(userName(closure.getApplicantId()));
        dto.setClosureType(closure.getClosureType());
        dto.setClosureTypeDesc(label(CLOSURE_TYPES, closure.getClosureType()));
        dto.setCaseOutcome(closure.getCaseOutcome());
        dto.setClosureSummary(closure.getClosureSummary());
        dto.setFeeStatus(closure.getFeeStatus());
        dto.setFeeStatusDesc(label(FEE_STATUSES, closure.getFeeStatus()));
        dto.setClientDeliveryStatus(closure.getClientDeliveryStatus());
        dto.setClientDeliveryStatusDesc(label(DELIVERY_STATUSES, closure.getClientDeliveryStatus()));
        dto.setClientDeliveryNotes(closure.getClientDeliveryNotes());
        dto.setDocumentsConfirmed(closure.getDocumentsConfirmed());
        dto.setStatus(closure.getStatus());
        dto.setRequestedAt(closure.getRequestedAt());
        dto.setReviewedBy(closure.getReviewedBy());
        dto.setReviewedByName(userName(closure.getReviewedBy()));
        dto.setReviewedAt(closure.getReviewedAt());
        dto.setReviewNotes(closure.getReviewNotes());

        List<CaseClosureDocument> links = closureDocumentRepository
                .findByClosureRequestIdAndDeletedFalseOrderByIdAsc(closure.getId());
        Map<Long, CaseDocument> documents = caseDocumentRepository.findAllById(links.stream()
                .map(CaseClosureDocument::getCaseDocumentId).collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(CaseDocument::getId, Function.identity()));
        dto.setBasisDocuments(links.stream().map(link -> {
            CaseDocument document = documents.get(link.getCaseDocumentId());
            if (document == null) return null;
            CaseClosureDocumentDTO item = new CaseClosureDocumentDTO();
            item.setDocumentId(document.getId());
            item.setDocumentName(document.getOriginalFileName());
            item.setDocumentType(document.getDocumentType());
            item.setFolderPath(document.getFolderPath());
            return item;
        }).filter(Objects::nonNull).collect(Collectors.toList()));
        return dto;
    }

    private void recordFlow(Long approvalId, Long userId, String action, String comments) {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setApprovalId(approvalId);
        flow.setApproverId(userId);
        flow.setAction(action);
        flow.setComments(comments);
        flow.setActionTime(LocalDateTime.now());
        approvalFlowRepository.save(flow);
    }

    private void completeReviewTodo(CaseClosureRequest closure, LocalDateTime completedAt) {
        if (closure.getReviewTodoId() == null) return;
        todoRepository.findById(closure.getReviewTodoId()).ifPresent(todo -> {
            todo.setStatus("COMPLETED");
            todo.setCompletedAt(completedAt);
            todoRepository.save(todo);
        });
    }

    private CaseClosureRequest requireByApprovalId(Long approvalId) {
        return closureRequestRepository.findByApprovalIdAndDeletedFalse(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("结案申请", approvalId));
    }

    private Case requireCase(Long caseId) {
        return caseRepository.findById(caseId).filter(value -> !Boolean.TRUE.equals(value.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("案件", caseId));
    }

    private String userName(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(User::getRealName).orElse("未知");
    }

    private static void requireCode(Map<String, String> values, String code, String field, String label) {
        if (!values.containsKey(code)) throw new InvalidParameterException(field, "请选择有效的" + label);
    }

    private static String label(Map<String, String> values, String code) {
        return values.getOrDefault(code, code == null ? "-" : code);
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String value(String value) {
        return hasText(value) ? value : "-";
    }

    private static Map<String, String> orderedMap(String... values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) result.put(values[index], values[index + 1]);
        return Collections.unmodifiableMap(result);
    }
}
