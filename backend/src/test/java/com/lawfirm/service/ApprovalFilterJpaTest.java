package com.lawfirm.service;

import com.lawfirm.converter.EncryptConverter;
import com.lawfirm.dto.ApprovalQueryRequest;
import com.lawfirm.entity.Approval;
import com.lawfirm.entity.User;
import com.lawfirm.mapper.KnowledgeArticleMapper;
import com.lawfirm.repository.ApprovalFlowRepository;
import com.lawfirm.repository.ApprovalRepository;
import com.lawfirm.repository.CaseDocumentRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.ConflictCheckRecordRepository;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:approval_filter;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "crypto.secret-key=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
})
@Import({CryptoUtil.class, EncryptConverter.class})
class ApprovalFilterJpaTest {

    @Autowired
    private ApprovalRepository approvalRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private KnowledgeArticleMapper knowledgeArticleMapper;

    private ApprovalService service;
    private User lawyer;
    private User administrative;

    @BeforeEach
    void setUp() {
        approvalRepository.deleteAll();
        userRepository.deleteAll();
        lawyer = userRepository.save(user("lawyer-filter", "律师甲", "律师"));
        administrative = userRepository.save(user("admin-filter", "行政甲", "行政管理"));
        service = new ApprovalService(
                approvalRepository,
                mock(ApprovalFlowRepository.class),
                userRepository,
                mock(CaseRepository.class),
                mock(CaseDocumentRepository.class),
                mock(ConflictCheckRecordRepository.class),
                mock(NotificationService.class),
                mock(CaseTimelineService.class),
                mock(CaseFileLibraryService.class),
                mock(UserPermissionService.class),
                mock(ConflictWaiverAttachmentService.class),
                mock(SealAttachmentService.class));
    }

    @Test
    void processedGroupAndDateRangeAreAppliedBeforePagination() {
        saveApproval("范围内已通过", "APPROVED", lawyer.getId(), administrative.getId(),
                LocalDateTime.of(2026, 7, 20, 10, 0));
        saveApproval("范围外已驳回", "REJECTED", lawyer.getId(), administrative.getId(),
                LocalDateTime.of(2026, 7, 10, 10, 0));
        saveApproval("范围内待审批", "PENDING", lawyer.getId(), administrative.getId(),
                LocalDateTime.of(2026, 7, 20, 11, 0));

        ApprovalQueryRequest request = new ApprovalQueryRequest();
        request.setStatusGroup("PROCESSED");
        request.setStartDate(LocalDate.of(2026, 7, 15));
        request.setEndDate(LocalDate.of(2026, 7, 23));

        var result = service.getApprovalList(request, lawyer.getId());

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).extracting("title").containsExactly("范围内已通过");
    }

    @Test
    void keywordMatchesApplicantNameWithinVisibleApprovals() {
        saveApproval("普通标题", "PENDING", administrative.getId(), lawyer.getId(),
                LocalDateTime.of(2026, 7, 20, 10, 0));
        saveApproval("不应命中", "PENDING", lawyer.getId(), administrative.getId(),
                LocalDateTime.of(2026, 7, 20, 11, 0));

        ApprovalQueryRequest request = new ApprovalQueryRequest();
        request.setKeyword("行政甲");

        var result = service.getApprovalList(request, lawyer.getId());

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).extracting("title").containsExactly("普通标题");
    }

    private User user(String username, String realName, String position) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("not-used-in-test");
        user.setRealName(realName);
        user.setPosition(position);
        user.setStatus(1);
        return user;
    }

    private void saveApproval(
            String title,
            String status,
            Long applicantId,
            Long approverId,
            LocalDateTime applyTime) {
        Approval approval = new Approval();
        approval.setApprovalType(ApprovalService.TYPE_CASE_FILING);
        approval.setTitle(title);
        approval.setContent("测试内容");
        approval.setApplicantId(applicantId);
        approval.setCurrentApproverId(approverId);
        approval.setStatus(status);
        approval.setApplyTime(applyTime);
        approvalRepository.save(approval);
    }
}
