package com.lawfirm.service;

import com.lawfirm.dto.ClientDTO;
import com.lawfirm.dto.ConflictCheckReviewRequest;
import com.lawfirm.dto.ConflictCheckResultDTO;
import com.lawfirm.entity.Client;
import com.lawfirm.entity.ConflictCheckRecord;
import com.lawfirm.entity.ClientSubjectRelation;
import com.lawfirm.entity.Party;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.ClientRepository;
import com.lawfirm.repository.CommunicationRecordRepository;
import com.lawfirm.repository.ConflictCheckRecordRepository;
import com.lawfirm.repository.DepartmentRepository;
import com.lawfirm.repository.PartyRepository;
import com.lawfirm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientConflictCheckServiceTest {

    private ClientRepository clientRepository;
    private PartyRepository partyRepository;
    private ConflictCheckRecordRepository recordRepository;
    private ConflictWaiverAttachmentService waiverAttachmentService;
    private ClientSubjectRelationService subjectRelationService;
    private ClientService service;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        partyRepository = mock(PartyRepository.class);
        recordRepository = mock(ConflictCheckRecordRepository.class);
        waiverAttachmentService = mock(ConflictWaiverAttachmentService.class);
        subjectRelationService = mock(ClientSubjectRelationService.class);
        service = new ClientService(
                clientRepository,
                mock(CaseRepository.class),
                mock(UserRepository.class),
                mock(CommunicationRecordRepository.class),
                partyRepository,
                mock(com.lawfirm.repository.CaseMemberRepository.class),
                mock(DepartmentRepository.class),
                recordRepository,
                mock(UserPermissionService.class),
                waiverAttachmentService,
                subjectRelationService);

        when(clientRepository.findByDeletedFalse()).thenReturn(Collections.emptyList());
        when(partyRepository.findByDeletedFalse()).thenReturn(Collections.emptyList());
        when(subjectRelationService.findAllActive()).thenReturn(Collections.emptyList());
        when(recordRepository.save(any(ConflictCheckRecord.class))).thenAnswer(invocation -> {
            ConflictCheckRecord record = invocation.getArgument(0);
            record.setId(12L);
            record.setCreatedAt(LocalDateTime.of(2026, 7, 23, 10, 30));
            return record;
        });
    }

    @Test
    void findsExactSubjectAcrossCasePartyLibraryAndCreatesTraceableRecord() {
        Party party = new Party();
        party.setId(21L);
        party.setCaseId(9L);
        party.setName("佛山某银行");
        party.setPartyRole("被告");
        party.setPartyType("单位");
        party.setDeleted(false);
        when(partyRepository.findByDeletedFalse()).thenReturn(List.of(party));

        ClientDTO request = new ClientDTO();
        request.setClientName("佛山某银行");
        ConflictCheckResultDTO result = service.checkConflictPreviewAndRecord(request, 1L);

        assertTrue(result.getHasConflict());
        assertEquals("CASE_PARTY", result.getConflictLevel());
        assertEquals(1, result.getHits().size());
        assertEquals("CASE_PARTY", result.getHits().get(0).getSourceType());
        assertEquals(List.of(9L), result.getConflictCaseIds());
        assertEquals("LC-20260723-000012", result.getReportNo());
    }

    @Test
    void detectsExistingClientByUnifiedCreditCode() {
        Client existing = new Client();
        existing.setId(31L);
        existing.setClientName("广东示例科技有限公司");
        existing.setCreditCode("91440000TEST001");
        existing.setDeleted(false);
        when(clientRepository.findByDeletedFalse()).thenReturn(List.of(existing));
        when(partyRepository.findByNameAndDeletedFalse(existing.getClientName())).thenReturn(Collections.emptyList());

        ClientDTO request = new ClientDTO();
        request.setClientName("示例科技");
        request.setCreditCode("91440000TEST001");
        ConflictCheckResultDTO result = service.checkConflictPreview(request);

        assertEquals("EXISTING", result.getConflictLevel());
        assertEquals("IDENTITY", result.getHits().get(0).getMatchType());
    }

    @Test
    void normalizesCompanySuffixForSimilarNameWarning() {
        Client existing = new Client();
        existing.setId(32L);
        existing.setClientName("广东至高实业有限公司");
        existing.setDeleted(false);
        when(clientRepository.findByDeletedFalse()).thenReturn(List.of(existing));

        ClientDTO request = new ClientDTO();
        request.setClientName("至高实业公司");
        ConflictCheckResultDTO result = service.checkConflictPreview(request);

        assertEquals("SIMILAR", result.getConflictLevel());
        assertEquals(List.of("广东至高实业有限公司"), result.getSimilarClientNames());
    }

    @Test
    void detectsRelatedSubjectThroughExplicitRelationshipGraph() {
        Client group = new Client();
        group.setId(40L);
        group.setClientName("广东示例控股集团有限公司");
        group.setDeleted(false);
        ClientSubjectRelation relation = new ClientSubjectRelation();
        relation.setId(51L);
        relation.setSourceClientId(40L);
        relation.setTargetSubjectName("佛山示例项目有限公司");
        relation.setRelationType("SUBSIDIARY");
        relation.setDeleted(false);
        when(clientRepository.findByDeletedFalse()).thenReturn(List.of(group));
        when(subjectRelationService.findAllActive()).thenReturn(List.of(relation));
        when(subjectRelationService.relationTypeName("SUBSIDIARY")).thenReturn("子公司");
        when(subjectRelationService.inverseRelationTypeName("SUBSIDIARY")).thenReturn("母公司");

        ClientDTO request = new ClientDTO();
        request.setClientName("佛山示例项目有限公司");
        ConflictCheckResultDTO result = service.checkConflictPreviewAndRecord(request, 3L);

        assertTrue(result.getHasConflict());
        assertEquals("RELATED", result.getConflictLevel());
        assertEquals("RELATED_ENTITY", result.getHits().get(0).getSourceType());
        assertEquals("广东示例控股集团有限公司", result.getHits().get(0).getSubjectName());
        assertEquals("母公司", result.getHits().get(0).getSubjectRole());
        assertTrue(result.getRecommendation().contains("关联主体"));
    }

    @Test
    void linksFilingConflictCheckToCaseBeforeApproval() {
        ClientDTO request = new ClientDTO();
        request.setClientName("案件委托方");
        request.setClientType("单位");

        service.checkConflictPreviewAndRecord(request, 3L, 88L);

        org.mockito.ArgumentCaptor<ConflictCheckRecord> captor =
                org.mockito.ArgumentCaptor.forClass(ConflictCheckRecord.class);
        verify(recordRepository).save(captor.capture());
        assertEquals(88L, captor.getValue().getCaseId());
        assertEquals("PENDING_REVIEW", captor.getValue().getReviewStatus());
    }

    @Test
    void completesFormalReviewAndLocksReviewerDetails() {
        ConflictCheckRecord record = pendingRecord();
        when(recordRepository.findById(12L)).thenReturn(Optional.of(record));

        ConflictCheckReviewRequest request = new ConflictCheckReviewRequest();
        request.setDecision("passed");
        request.setConclusion("已核对现有客户和案件当事人，无禁止代理情形。 ");

        service.reviewConflictCheck(12L, request, 8L);

        assertEquals("COMPLETED", record.getReviewStatus());
        assertEquals("PASSED", record.getReviewDecision());
        assertEquals("已核对现有客户和案件当事人，无禁止代理情形。", record.getReviewConclusion());
        assertEquals(8L, record.getReviewedBy());
        assertNotNull(record.getReviewedAt());
    }

    @Test
    void requiresWaiverBasisForConditionalApproval() {
        ConflictCheckRecord record = pendingRecord();
        when(recordRepository.findById(12L)).thenReturn(Optional.of(record));

        ConflictCheckReviewRequest request = new ConflictCheckReviewRequest();
        request.setDecision("CONDITIONAL");
        request.setConclusion("存在关联关系，经风险隔离后可办理。 ");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.reviewConflictCheck(12L, request, 8L));

        assertTrue(exception.getMessage().contains("书面豁免或风险处置依据"));
        assertEquals("PENDING_REVIEW", record.getReviewStatus());
    }

    @Test
    void requiresWaiverOriginalForConditionalApproval() {
        ConflictCheckRecord record = pendingRecord();
        when(recordRepository.findById(12L)).thenReturn(Optional.of(record));

        ConflictCheckReviewRequest request = new ConflictCheckReviewRequest();
        request.setDecision("CONDITIONAL");
        request.setConclusion("经信息隔离后可以办理");
        request.setWaiverBasis("客户已出具书面知情同意");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.reviewConflictCheck(12L, request, 8L));

        assertTrue(exception.getMessage().contains("必须上传"));
        assertEquals("PENDING_REVIEW", record.getReviewStatus());
    }

    @Test
    void acceptsConditionalApprovalAfterWaiverOriginalUploaded() {
        ConflictCheckRecord record = pendingRecord();
        when(recordRepository.findById(12L)).thenReturn(Optional.of(record));
        when(waiverAttachmentService.hasAttachment(12L)).thenReturn(true);

        ConflictCheckReviewRequest request = new ConflictCheckReviewRequest();
        request.setDecision("CONDITIONAL");
        request.setConclusion("经信息隔离后可以办理");
        request.setWaiverBasis("客户已出具书面知情同意");

        service.reviewConflictCheck(12L, request, 8L);

        assertEquals("COMPLETED", record.getReviewStatus());
        assertEquals("CONDITIONAL", record.getReviewDecision());
    }

    @Test
    void rejectsOverwritingCompletedFormalReview() {
        ConflictCheckRecord record = pendingRecord();
        record.setReviewStatus("COMPLETED");
        record.setReviewDecision("REJECTED");
        when(recordRepository.findById(12L)).thenReturn(Optional.of(record));

        ConflictCheckReviewRequest request = new ConflictCheckReviewRequest();
        request.setDecision("PASSED");
        request.setConclusion("尝试覆盖原审查结论");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.reviewConflictCheck(12L, request, 8L));

        assertTrue(exception.getMessage().contains("不允许覆盖"));
        assertEquals("REJECTED", record.getReviewDecision());
    }

    private ConflictCheckRecord pendingRecord() {
        ConflictCheckRecord record = new ConflictCheckRecord();
        record.setId(12L);
        record.setSubjectName("测试主体");
        record.setCheckedBy(3L);
        record.setReviewStatus("PENDING_REVIEW");
        record.setCreatedAt(LocalDateTime.of(2026, 7, 23, 10, 30));
        return record;
    }
}
