package com.lawfirm.service;

import com.lawfirm.dto.ApprovalQueryRequest;
import com.lawfirm.entity.Approval;
import com.lawfirm.entity.ApprovalFlow;
import com.lawfirm.entity.User;
import com.lawfirm.repository.ApprovalFlowRepository;
import com.lawfirm.repository.ApprovalRepository;
import com.lawfirm.repository.CaseDocumentRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalServiceTest {

    private ApprovalRepository approvalRepository;
    private ApprovalFlowRepository approvalFlowRepository;
    private UserRepository userRepository;
    private NotificationService notificationService;
    private ApprovalService service;

    @BeforeEach
    void setUp() {
        approvalRepository = mock(ApprovalRepository.class);
        approvalFlowRepository = mock(ApprovalFlowRepository.class);
        userRepository = mock(UserRepository.class);
        notificationService = mock(NotificationService.class);
        service = new ApprovalService(
                approvalRepository,
                approvalFlowRepository,
                userRepository,
                mock(CaseRepository.class),
                mock(CaseDocumentRepository.class),
                notificationService,
                mock(CaseTimelineService.class),
                mock(CaseFileLibraryService.class));
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
    void caseFilingApprovalRequiresReviewOpinion() {
        Approval approval = pendingApproval(11L, ApprovalService.TYPE_CASE_FILING, 7L, 8L);
        when(approvalRepository.findById(11L)).thenReturn(Optional.of(approval));
        when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, "行政甲", "行政管理")));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.approveApproval(11L, "  ", 8L));

        assertEquals("立案审批意见不能为空", error.getMessage());
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

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.approveApproval(13L, "同意", 9L));

        assertEquals("您不是当前审批人", error.getMessage());
    }

    @Test
    void developmentAdminCanHandleApprovalAssignedToAnotherUser() {
        Approval approval = pendingApproval(14L, ApprovalService.TYPE_SEAL, 7L, 8L);
        when(approvalRepository.findById(14L)).thenReturn(Optional.of(approval));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin", "行政管理")));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "律师甲", "律师")));

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
