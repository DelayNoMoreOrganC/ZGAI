package com.lawfirm.service;

import com.lawfirm.dto.ApprovalQueryRequest;
import com.lawfirm.dto.ApprovalAttachmentDTO;
import com.lawfirm.entity.Approval;
import com.lawfirm.entity.ApprovalFlow;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.ConflictCheckRecord;
import com.lawfirm.entity.LawFirmLetter;
import com.lawfirm.entity.User;
import com.lawfirm.event.SealApprovalDecisionEvent;
import com.lawfirm.repository.ApprovalFlowRepository;
import com.lawfirm.repository.ApprovalRepository;
import com.lawfirm.repository.CaseDocumentRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.ConflictCheckRecordRepository;
import com.lawfirm.repository.LawFirmLetterRepository;
import com.lawfirm.repository.LawFirmLetterSequenceRepository;
import com.lawfirm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

class ApprovalServiceTest {

    private ApprovalRepository approvalRepository;
    private ApprovalFlowRepository approvalFlowRepository;
    private UserRepository userRepository;
    private CaseRepository caseRepository;
    private CaseDocumentRepository caseDocumentRepository;
    private ConflictCheckRecordRepository conflictCheckRecordRepository;
    private NotificationService notificationService;
    private UserPermissionService userPermissionService;
    private CaseFileLibraryService caseFileLibraryService;
    private ConflictWaiverAttachmentService conflictWaiverAttachmentService;
    private SealAttachmentService sealAttachmentService;
    private LawFirmLetterRepository lawFirmLetterRepository;
    private LawFirmLetterSequenceRepository lawFirmLetterSequenceRepository;
    private ApplicationEventPublisher eventPublisher;
    private ApprovalService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        approvalRepository = mock(ApprovalRepository.class);
        approvalFlowRepository = mock(ApprovalFlowRepository.class);
        userRepository = mock(UserRepository.class);
        caseRepository = mock(CaseRepository.class);
        caseDocumentRepository = mock(CaseDocumentRepository.class);
        conflictCheckRecordRepository = mock(ConflictCheckRecordRepository.class);
        notificationService = mock(NotificationService.class);
        userPermissionService = mock(UserPermissionService.class);
        caseFileLibraryService = mock(CaseFileLibraryService.class);
        conflictWaiverAttachmentService = mock(ConflictWaiverAttachmentService.class);
        sealAttachmentService = mock(SealAttachmentService.class);
        lawFirmLetterRepository = mock(LawFirmLetterRepository.class);
        lawFirmLetterSequenceRepository = mock(LawFirmLetterSequenceRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new ApprovalService(
                approvalRepository,
                approvalFlowRepository,
                userRepository,
                caseRepository,
                caseDocumentRepository,
                conflictCheckRecordRepository,
                notificationService,
                mock(CaseTimelineService.class),
                caseFileLibraryService,
                userPermissionService,
                conflictWaiverAttachmentService,
                sealAttachmentService,
                mock(CaseClosureService.class),
                lawFirmLetterRepository,
                lawFirmLetterSequenceRepository);
        service.setApplicationEventPublisher(eventPublisher);
    }

    @Test
    void firstApiPageUsesFirstDatabasePage() {
        ApprovalQueryRequest request = new ApprovalQueryRequest();
        request.setPage(1);
        request.setSize(20);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "律师甲", "律师")));
        when(approvalRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(1);
                    assertEquals(0, pageable.getPageNumber());
                    return new PageImpl<Approval>(Collections.emptyList(), pageable, 0);
                });

        var result = service.getApprovalList(request, 7L);

        assertEquals(false, result.getHasPrevious());
        assertEquals(false, result.getHasNext());
        verify(approvalRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void managerRoleCanViewApprovalWithoutDirectorPositionLabel() {
        User manager = user(9L, "主任甲", "部门负责人");
        Approval approval = pendingApproval(90L, ApprovalService.TYPE_CASE_FILING_DIRECTOR, 7L, 8L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(manager));
        when(userPermissionService.hasRole(manager, "MANAGER")).thenReturn(true);
        when(approvalRepository.findById(90L)).thenReturn(Optional.of(approval));

        Approval result = approvalRepository.findById(90L).orElseThrow();
        assertEquals(90L, service.getApprovalDetail(result.getId(), 9L).getId());
    }

    @Test
    void approvalKeywordSearchResolvesApplicantNames() {
        ApprovalQueryRequest request = new ApprovalQueryRequest();
        request.setKeyword("  行政甲  ");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "律师甲", "律师")));
        when(userRepository.findActiveIdsByNameOrUsername("行政甲"))
                .thenReturn(Collections.singletonList(8L));
        when(approvalRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        service.getApprovalList(request, 7L);

        verify(userRepository).findActiveIdsByNameOrUsername("行政甲");
        verify(approvalRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void caseFilingApprovalRequiresReviewOpinion() {
        Approval approval = pendingApproval(11L, ApprovalService.TYPE_CASE_FILING, 7L, 8L);
        when(approvalRepository.findById(11L)).thenReturn(Optional.of(approval));
        when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, "行政甲", "行政管理")));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.approveApproval(11L, "  ", 8L));

        assertEquals("立案审批意见不能为空", error.getMessage());
    }

    @Test
    void sealApprovalRequiresRecordedOpinionAndUpdatesAttachmentStatus() {
        Approval approval = pendingApproval(111L, ApprovalService.TYPE_SEAL, 7L, 8L);
        when(approvalRepository.findById(111L)).thenReturn(Optional.of(approval));
        when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, "行政甲", "行政管理")));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "律师甲", "律师")));
        when(sealAttachmentService.list(111L)).thenReturn(Collections.singletonList(new ApprovalAttachmentDTO()));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.approveApproval(111L, " ", 8L));
        assertEquals("用印审批意见不能为空", error.getMessage());

        service.approveApproval(111L, "文件内容无误，同意用印", 8L);

        assertEquals("APPROVED", approval.getStatus());
        verify(sealAttachmentService).markDecision(
                org.mockito.ArgumentMatchers.eq(111L),
                org.mockito.ArgumentMatchers.eq("APPROVED"),
                org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    void firstLawFirmLetterApprovalRequiresAdministrativeInitialSerial() {
        Approval approval = pendingApproval(112L, ApprovalService.TYPE_SEAL, 7L, 8L);
        LawFirmLetter letter = new LawFirmLetter();
        letter.setId(21L);
        letter.setLetterTypeCode("民");
        when(approvalRepository.findById(112L)).thenReturn(Optional.of(approval));
        when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, "行政甲", "行政管理")));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "律师甲", "律师")));
        when(sealAttachmentService.list(112L)).thenReturn(Collections.singletonList(new ApprovalAttachmentDTO()));
        when(lawFirmLetterRepository.findByApprovalIdAndDeletedFalse(112L)).thenReturn(Optional.of(letter));
        when(lawFirmLetterSequenceRepository.findByLetterYearAndLetterTypeCode(any(), any()))
                .thenReturn(Optional.empty());

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.approveApproval(112L, "文件内容无误，同意用印", null, 8L));
        assertTrue(error.getMessage().contains("该年度和函种尚未编号，请先填写首次流水号"));

        approval.setStatus("PENDING");
        service.approveApproval(112L, "文件内容无误，同意用印", 25, 8L);
        verify(eventPublisher).publishEvent(any(SealApprovalDecisionEvent.class));
    }

    @Test
    void rejectionRequiresReason() {
        Approval approval = pendingApproval(12L, ApprovalService.TYPE_SEAL, 7L, 8L);
        when(approvalRepository.findById(12L)).thenReturn(Optional.of(approval));
        when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, "行政甲", "行政管理")));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.rejectApproval(12L, "", 8L));

        assertEquals("驳回理由不能为空", error.getMessage());
    }

    @Test
    void unrelatedLawyerCannotHandleApproval() {
        Approval approval = pendingApproval(13L, ApprovalService.TYPE_SEAL, 7L, 8L);
        when(approvalRepository.findById(13L)).thenReturn(Optional.of(approval));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, "律师乙", "律师")));

        AccessDeniedException error = assertThrows(AccessDeniedException.class,
                () -> service.approveApproval(13L, "同意", 9L));

        assertEquals("您不是当前审批人", error.getMessage());
    }

    @Test
    void developmentAdminCanHandleApprovalAssignedToAnotherUser() {
        Approval approval = pendingApproval(14L, ApprovalService.TYPE_SEAL, 7L, 8L);
        when(approvalRepository.findById(14L)).thenReturn(Optional.of(approval));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin", "行政管理")));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "律师甲", "律师")));
        when(sealAttachmentService.list(14L)).thenReturn(Collections.singletonList(new ApprovalAttachmentDTO()));

        service.approveApproval(14L, "同意", 1L);

        assertEquals("APPROVED", approval.getStatus());
        verify(approvalRepository).save(approval);
        verify(approvalFlowRepository).save(any(ApprovalFlow.class));
    }

    @Test
    void approvalFlowIncludesOperatorName() {
        Approval approval = pendingApproval(15L, ApprovalService.TYPE_SEAL, 7L, 8L);
        ApprovalFlow flow = new ApprovalFlow();
        flow.setId(20L);
        flow.setApprovalId(15L);
        flow.setApproverId(7L);
        flow.setAction("SUBMIT");
        flow.setActionTime(LocalDateTime.now());
        when(approvalRepository.findById(15L)).thenReturn(Optional.of(approval));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "律师甲", "律师")));
        when(approvalFlowRepository.findByApprovalIdOrderByActionTimeAsc(15L))
                .thenReturn(Collections.singletonList(flow));

        var result = service.getApprovalFlow(15L, 7L);

        assertEquals("律师甲", result.get(0).getApproverName());
    }

    @Test
    void ordinaryFeeCaseStillFlowsFromAdministrativeReviewToDirectorFinalReview() {
        Approval approval = pendingApproval(16L, ApprovalService.TYPE_CASE_FILING, 7L, 8L);
        approval.setCaseId(100L);
        Case caseEntity = new Case();
        caseEntity.setId(100L);
        caseEntity.setCaseName("普通收费案件");
        caseEntity.setFeeMethod("FIXED");
        caseEntity.setStatus("PENDING_APPROVAL");
        User administrative = user(8L, "行政甲", "行政管理1");
        User director = user(9L, "主任甲", "主任");

        when(approvalRepository.findById(16L)).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(Approval.class))).thenAnswer(invocation -> {
            Approval saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId(17L);
            return saved;
        });
        when(userRepository.findById(8L)).thenReturn(Optional.of(administrative));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "律师甲", "律师")));
        when(userRepository.findById(9L)).thenReturn(Optional.of(director));
        when(caseRepository.findById(100L)).thenReturn(Optional.of(caseEntity));
        when(userPermissionService.findFirstActiveUserByPermission("CASE_FILING_FINAL_APPROVE", "MANAGER"))
                .thenReturn(Optional.of(director));

        service.approveApproval(16L, "利冲无异常，同意初审", 8L);

        org.mockito.ArgumentCaptor<Approval> captor = org.mockito.ArgumentCaptor.forClass(Approval.class);
        verify(approvalRepository, times(2)).save(captor.capture());
        Approval directorApproval = captor.getAllValues().get(1);
        assertEquals(ApprovalService.TYPE_CASE_FILING_DIRECTOR, directorApproval.getApprovalType());
        assertEquals(9L, directorApproval.getCurrentApproverId());
        assertEquals("PENDING", directorApproval.getStatus());
        assertEquals("PENDING_APPROVAL", caseEntity.getStatus());
        assertTrue(directorApproval.getContent().contains("收费方式：固定收费"));
    }

    @Test
    void existingFilingApprovalShowsCurrentRegisteredFeeDetails() {
        Approval approval = pendingApproval(17L, ApprovalService.TYPE_CASE_FILING, 7L, 8L);
        approval.setCaseId(100L);
        approval.setContent("案件名称：收费展示案件\n收费方式：固定收费\n主办律师：律师甲");
        Case caseEntity = new Case();
        caseEntity.setId(100L);
        caseEntity.setCaseName("收费展示案件");
        caseEntity.setFeeMethod("BASE_PLUS_CONTINGENT");
        caseEntity.setAttorneyFee(new java.math.BigDecimal("30000.00"));
        caseEntity.setRiskRatio(new java.math.BigDecimal("2.00"));

        when(approvalRepository.findById(17L)).thenReturn(Optional.of(approval));
        when(caseRepository.findById(100L)).thenReturn(Optional.of(caseEntity));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "律师甲", "律师")));
        when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, "行政甲", "行政管理")));
        when(conflictCheckRecordRepository.findByCaseIdOrderByCreatedAtAsc(100L))
                .thenReturn(Collections.emptyList());

        var detail = service.getApprovalDetail(17L, 7L);

        assertTrue(detail.getContent().contains("收费方式：固定收费 30000元+风险收费 2%"));
    }

    @Test
    void administrativeApprovalWaitsForAllFormalConflictReviews() {
        Approval approval = pendingApproval(18L, ApprovalService.TYPE_CASE_FILING, 7L, 8L);
        approval.setCaseId(101L);
        Case caseEntity = new Case();
        caseEntity.setId(101L);
        caseEntity.setCaseName("待利冲复核案件");
        ConflictCheckRecord pending = new ConflictCheckRecord();
        pending.setId(31L);
        pending.setCaseId(101L);
        pending.setSubjectName("委托方甲");
        pending.setReviewStatus("PENDING_REVIEW");

        when(approvalRepository.findById(18L)).thenReturn(Optional.of(approval));
        when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, "行政甲", "行政管理")));
        when(caseRepository.findById(101L)).thenReturn(Optional.of(caseEntity));
        when(conflictCheckRecordRepository.findByCaseIdOrderByCreatedAtAsc(101L))
                .thenReturn(Collections.singletonList(pending));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.approveApproval(18L, "同意立案", 8L));

        assertEquals("请先完成全部委托方的正式利冲审查，再同意立案", error.getMessage());
    }

    @Test
    void rejectedFormalConflictReviewCannotBeApproved() {
        Approval approval = pendingApproval(19L, ApprovalService.TYPE_CASE_FILING, 7L, 8L);
        approval.setCaseId(102L);
        Case caseEntity = new Case();
        caseEntity.setId(102L);
        caseEntity.setCaseName("利冲不通过案件");
        ConflictCheckRecord rejected = new ConflictCheckRecord();
        rejected.setId(32L);
        rejected.setCaseId(102L);
        rejected.setSubjectName("委托方乙");
        rejected.setReviewStatus("COMPLETED");
        rejected.setReviewDecision("REJECTED");

        when(approvalRepository.findById(19L)).thenReturn(Optional.of(approval));
        when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, "行政甲", "行政管理")));
        when(caseRepository.findById(102L)).thenReturn(Optional.of(caseEntity));
        when(conflictCheckRecordRepository.findByCaseIdOrderByCreatedAtAsc(102L))
                .thenReturn(Collections.singletonList(rejected));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.approveApproval(19L, "同意立案", 8L));

        assertEquals("正式利冲审查存在不通过结论，不能同意立案；请驳回申请并说明原因", error.getMessage());
    }

    @Test
    void conditionalFormalReviewWithoutOriginalCannotBeApproved() {
        Approval approval = pendingApproval(21L, ApprovalService.TYPE_CASE_FILING, 7L, 8L);
        approval.setCaseId(104L);
        Case caseEntity = new Case();
        caseEntity.setId(104L);
        caseEntity.setCaseName("附条件利冲案件");
        ConflictCheckRecord conditional = new ConflictCheckRecord();
        conditional.setId(34L);
        conditional.setCaseId(104L);
        conditional.setSubjectName("委托方丁");
        conditional.setReviewStatus("COMPLETED");
        conditional.setReviewDecision("CONDITIONAL");

        when(approvalRepository.findById(21L)).thenReturn(Optional.of(approval));
        when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, "行政甲", "行政管理")));
        when(caseRepository.findById(104L)).thenReturn(Optional.of(caseEntity));
        when(conflictCheckRecordRepository.findByCaseIdOrderByCreatedAtAsc(104L))
                .thenReturn(Collections.singletonList(conditional));
        when(conflictWaiverAttachmentService.hasAttachment(34L)).thenReturn(false);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.approveApproval(21L, "同意立案", 8L));

        assertEquals("附条件通过的利冲审查缺少书面豁免或风险处置依据原件", error.getMessage());
    }

    @Test
    void directorFinalApprovalArchivesStructuredConflictReport() {
        Approval approval = pendingApproval(20L, ApprovalService.TYPE_CASE_FILING_DIRECTOR, 7L, 9L);
        approval.setCaseId(103L);
        Case caseEntity = new Case();
        caseEntity.setId(103L);
        caseEntity.setCaseName("正式利冲归档案件");
        caseEntity.setCaseNumber("LS-2026-CIVIL-103");
        caseEntity.setCaseType("CIVIL");
        caseEntity.setStatus("PENDING_APPROVAL");
        caseEntity.setFilingDate(java.time.LocalDate.of(2020, 1, 1));
        ConflictCheckRecord reviewed = new ConflictCheckRecord();
        reviewed.setId(33L);
        reviewed.setCaseId(103L);
        reviewed.setSubjectName("委托方丙");
        reviewed.setConflictLevel("NONE");
        reviewed.setConclusion("未发现冲突线索");
        reviewed.setReviewStatus("COMPLETED");
        reviewed.setReviewDecision("PASSED");
        reviewed.setReviewConclusion("核对通过");
        reviewed.setReviewedBy(8L);
        reviewed.setReviewedAt(LocalDateTime.of(2026, 7, 23, 16, 0));
        reviewed.setCreatedAt(LocalDateTime.of(2026, 7, 23, 15, 0));

        when(approvalRepository.findById(20L)).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(Approval.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, "主任甲", "主任")));
        when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, "行政甲", "行政管理")));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "律师甲", "律师")));
        when(caseRepository.findById(103L)).thenReturn(Optional.of(caseEntity));
        when(conflictCheckRecordRepository.findByCaseIdOrderByCreatedAtAsc(103L))
                .thenReturn(Collections.singletonList(reviewed));
        when(caseFileLibraryService.ensureCaseFolder(caseEntity)).thenReturn(tempDir);
        when(caseDocumentRepository.save(any(com.lawfirm.entity.CaseDocument.class))).thenAnswer(invocation -> {
            com.lawfirm.entity.CaseDocument document = invocation.getArgument(0);
            document.setId(77L);
            return document;
        });

        service.approveApproval(20L, "同意终审", 9L);

        assertEquals("ACTIVE", caseEntity.getStatus());
        assertEquals(approval.getApprovedTime().toLocalDate(), caseEntity.getFilingDate());
        assertEquals(77L, reviewed.getArchivedDocumentId());
        assertNotNull(reviewed.getArchivedAt());
        assertTrue(Files.exists(tempDir.resolve("01_立案材料").resolve("利冲审查报告_LC-20260723-000033.txt")));
        verify(conflictCheckRecordRepository).save(reviewed);
    }

    private Approval pendingApproval(Long id, String type, Long applicantId, Long approverId) {
        Approval approval = new Approval();
        approval.setId(id);
        approval.setApprovalType(type);
        approval.setTitle("测试审批");
        approval.setContent("测试内容");
        approval.setApplicantId(applicantId);
        approval.setCurrentApproverId(approverId);
        approval.setStatus("PENDING");
        approval.setApplyTime(LocalDateTime.now());
        return approval;
    }

    private User user(Long id, String username, String position) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(username);
        user.setPosition(position);
        return user;
    }
}
