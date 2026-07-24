package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.CaseCreateRequest;
import com.lawfirm.dto.CaseQueryRequest;
import com.lawfirm.dto.CaseUpdateRequest;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.Client;
import com.lawfirm.entity.Role;
import com.lawfirm.entity.User;
import com.lawfirm.entity.UserRole;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.repository.CaseFlowTemplateRepository;
import com.lawfirm.repository.CaseMemberRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.CaseStageTodoTemplateRepository;
import com.lawfirm.repository.ClientRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CaseAccessPolicyTest {

    @Mock private CaseRepository caseRepository;
    @Mock private PartyService partyService;
    @Mock private CaseProcedureService caseProcedureService;
    @Mock private CaseRecordService caseRecordService;
    @Mock private CaseTimelineService caseTimelineService;
    @Mock private CaseMemberService caseMemberService;
    @Mock private CaseStageService caseStageService;
    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private CaseMemberRepository caseMemberRepository;
    @Mock private FinanceRecordService financeRecordService;
    @Mock private CaseFlowTemplateRepository caseFlowTemplateRepository;
    @Mock private CaseStageTodoTemplateRepository caseStageTodoTemplateRepository;
    @Mock private TodoService todoService;
    @Mock private ClientService clientService;
    @Mock private ClientRepository clientRepository;
    @Mock private ApprovalService approvalService;
    @Mock private ObjectMapper objectMapper;
    @Mock private UserPermissionService userPermissionService;

    @InjectMocks private CaseService caseService;

    @Test
    void ordinaryCaseEditCannotBypassStageClosureOrArchiveWorkflows() {
        Case caseEntity = activeCase(90L, 20L);
        when(caseRepository.findById(90L)).thenReturn(Optional.of(caseEntity));

        CaseUpdateRequest stageRequest = new CaseUpdateRequest();
        stageRequest.setCurrentStage("裁判");
        assertThrows(InvalidParameterException.class, () -> caseService.updateCase(90L, stageRequest));

        CaseUpdateRequest closeRequest = new CaseUpdateRequest();
        closeRequest.setCloseDate(LocalDate.now());
        assertThrows(InvalidParameterException.class, () -> caseService.updateCase(90L, closeRequest));

        CaseUpdateRequest archiveRequest = new CaseUpdateRequest();
        archiveRequest.setArchiveDate(LocalDate.now());
        assertThrows(InvalidParameterException.class, () -> caseService.updateCase(90L, archiveRequest));

        verify(caseRepository, never()).save(any(Case.class));
    }

    @Test
    void consultantNameUsesConsultantUnitAndServiceYear() {
        CaseCreateRequest request = new CaseCreateRequest();
        request.setCaseType("CONSULTANT");
        request.setConsultantUnitName("广东示例有限公司");
        request.setBusinessType("常年法律顾问");
        request.setServiceStartDate(LocalDate.of(2026, 1, 1));

        org.junit.jupiter.api.Assertions.assertEquals(
                "广东示例有限公司2026年度常年法律顾问", caseService.autoGenerateName(request));
    }

    @Test
    void civilCaseIgnoresStaleConsultantClientSelection() {
        CaseCreateRequest request = new CaseCreateRequest();
        request.setCaseType("CIVIL");
        request.setConsultantClientId(99L);
        request.setClientIds(List.of(12L));

        Long primaryClientId = ReflectionTestUtils.invokeMethod(caseService, "resolvePrimaryClientId", request);

        assertEquals(12L, primaryClientId);
    }

    @Test
    void consultantClientMustBeVisibleAndMustNotBeAnIndividual() {
        CaseCreateRequest request = new CaseCreateRequest();
        request.setCaseType("CONSULTANT");
        request.setConsultantClientId(88L);

        Client client = new Client();
        client.setId(88L);
        client.setClientName("张三");
        client.setClientType("个人");
        client.setDeleted(false);
        when(clientRepository.findById(88L)).thenReturn(Optional.of(client));

        assertThrows(InvalidParameterException.class,
                () -> ReflectionTestUtils.invokeMethod(caseService, "hydrateConsultantClient", request, 20L));
        verify(clientService).assertClientVisible(88L, 20L);
    }

    @Test
    void administrativeUserCanViewAllCasesButCannotEditThem() {
        Case caseEntity = activeCase(100L, 20L);
        User administrative = user(10L, "行政甲", "行政管理1", 9L);
        when(caseRepository.findById(100L)).thenReturn(Optional.of(caseEntity));
        when(userRepository.findById(10L)).thenReturn(Optional.of(administrative));

        assertTrue(caseService.canAccessCase(100L, 10L));
        assertThrows(AccessDeniedException.class, () -> caseService.assertCaseEditable(100L, 10L));
        assertThrows(AccessDeniedException.class, () -> caseService.assertCaseManageable(100L, 10L));
    }

    @Test
    void directorAndFilingAdministratorCanEditBeforeClosure() {
        Case caseEntity = activeCase(101L, 20L);
        User director = user(11L, "主任甲", "主任", 1L);
        User filingAdministrator = user(12L, "田颖思", "行政管理1", 9L);
        when(caseRepository.findById(101L)).thenReturn(Optional.of(caseEntity));
        when(userRepository.findById(11L)).thenReturn(Optional.of(director));
        when(userRepository.findById(12L)).thenReturn(Optional.of(filingAdministrator));
        when(userPermissionService.hasPermission(filingAdministrator, "CASE_FILING_MANAGE")).thenReturn(true);

        assertDoesNotThrow(() -> caseService.assertCaseEditable(101L, 11L));
        assertDoesNotThrow(() -> caseService.assertCaseEditable(101L, 12L));
    }

    @Test
    void managerRoleWithDepartmentHeadPositionHasFirmWideCaseAccess() {
        Case caseEntity = activeCase(108L, 20L);
        User director = user(13L, "主任乙", "部门负责人", 8L);
        UserRole assignment = new UserRole();
        assignment.setUserId(13L);
        assignment.setRoleId(6L);
        Role managerRole = new Role();
        managerRole.setId(6L);
        managerRole.setRoleCode("MANAGER");
        managerRole.setRoleName("主任");
        when(caseRepository.findById(108L)).thenReturn(Optional.of(caseEntity));
        when(userRepository.findById(13L)).thenReturn(Optional.of(director));
        when(userRoleRepository.findByUserId(13L)).thenReturn(List.of(assignment));
        when(roleRepository.findById(6L)).thenReturn(Optional.of(managerRole));

        assertTrue(caseService.canAccessCase(108L, 13L));
        assertDoesNotThrow(() -> caseService.assertCaseEditable(108L, 13L));
    }

    @Test
    void ordinaryLawyerCanEditOwnCaseButNotUnrelatedCase() {
        Case caseEntity = activeCase(102L, 20L);
        User owner = user(20L, "律师甲", "律师", 2L);
        User unrelated = user(21L, "律师乙", "律师", 3L);
        when(caseRepository.findById(102L)).thenReturn(Optional.of(caseEntity));
        when(userRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(21L)).thenReturn(Optional.of(unrelated));
        when(caseMemberRepository.findByCaseIdAndDeletedFalse(102L)).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> caseService.assertCaseEditable(102L, 20L));
        assertThrows(AccessDeniedException.class, () -> caseService.assertCaseVisible(102L, 21L));
        assertThrows(AccessDeniedException.class, () -> caseService.assertCaseEditable(102L, 21L));
    }

    @Test
    void departmentHeadCanEditDepartmentCase() {
        Case caseEntity = activeCase(103L, 30L);
        User departmentHead = user(22L, "主管甲", "主管", 4L);
        User owner = user(30L, "律师丙", "律师", 4L);
        when(caseRepository.findById(103L)).thenReturn(Optional.of(caseEntity));
        when(userRepository.findById(22L)).thenReturn(Optional.of(departmentHead));
        when(userRepository.findById(30L)).thenReturn(Optional.of(owner));
        when(caseMemberRepository.findByCaseIdAndDeletedFalse(103L)).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> caseService.assertCaseEditable(103L, 22L));
    }

    @Test
    void closedCaseIsLockedEvenForFilingAdministrator() {
        Case caseEntity = activeCase(104L, 20L);
        caseEntity.setStatus("CLOSED");
        User filingAdministrator = user(12L, "田颖思", "行政管理1", 9L);
        when(caseRepository.findById(104L)).thenReturn(Optional.of(caseEntity));
        when(userRepository.findById(12L)).thenReturn(Optional.of(filingAdministrator));

        assertThrows(InvalidParameterException.class, () -> caseService.assertCaseEditable(104L, 12L));
    }

    @Test
    void caseDetailReturnsEffectiveActionPermissions() {
        Case caseEntity = activeCase(105L, 20L);
        caseEntity.setCaseName("权限测试案件");
        User owner = user(20L, "律师甲", "律师", 2L);
        when(caseRepository.findById(105L)).thenReturn(Optional.of(caseEntity));
        when(userRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(caseMemberService.getByCaseIdAndType(105L, "CO_OWNER")).thenReturn(Collections.emptyList());
        when(caseMemberService.getByCaseIdAndType(105L, "ASSISTANT")).thenReturn(Collections.emptyList());
        when(partyService.getByCaseId(105L)).thenReturn(Collections.emptyList());
        when(caseProcedureService.getByCaseId(105L)).thenReturn(Collections.emptyList());
        when(caseStageService.getStageProgress(105L)).thenReturn(Collections.emptyList());
        when(clientService.getConflictCheckRecordsByCaseId(105L, null)).thenReturn(Collections.emptyList());
        when(clientService.getConflictCheckRecordsByCaseId(105L, 20L)).thenReturn(Collections.emptyList());
        when(userPermissionService.hasPermission(org.mockito.ArgumentMatchers.eq(owner), anyString()))
                .thenAnswer(invocation -> "CASE_EDIT".equals(invocation.getArgument(1)));

        com.lawfirm.vo.CaseDetailVO detail = caseService.getCaseDetail(105L, 20L);

        assertTrue(detail.getCanEdit());
        assertTrue(detail.getCanChangeStatus());
        assertFalse(detail.getCanDelete());
        assertFalse(detail.getCanArchive());
    }

    @Test
    @SuppressWarnings("unchecked")
    void caseListReturnsPerCaseEffectiveActionsAndHearingDate() {
        Case ownCase = activeCase(106L, 20L);
        ownCase.setCaseName("本人案件");
        ownCase.setHearingDate(LocalDate.of(2026, 8, 10));
        Case unrelatedCase = activeCase(107L, 30L);
        unrelatedCase.setCaseName("无关案件");
        User lawyer = user(20L, "律师甲", "律师", 2L);

        when(userRepository.findById(20L)).thenReturn(Optional.of(lawyer));
        when(userRepository.findById(30L)).thenReturn(Optional.empty());
        when(caseMemberRepository.findByUserIdInAndDeletedFalse(List.of(20L))).thenReturn(Collections.emptyList());
        when(caseMemberRepository.findByCaseIdAndDeletedFalse(107L)).thenReturn(Collections.emptyList());
        when(caseRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ownCase, unrelatedCase)));
        when(partyService.getByCaseId(106L)).thenReturn(Collections.emptyList());
        when(partyService.getByCaseId(107L)).thenReturn(Collections.emptyList());
        when(userPermissionService.hasPermission(org.mockito.ArgumentMatchers.eq(lawyer), anyString()))
                .thenAnswer(invocation -> !"CASE_FILING_MANAGE".equals(invocation.getArgument(1)));

        com.lawfirm.util.PageResult<com.lawfirm.vo.CaseListVO> result =
                caseService.getCaseList(new CaseQueryRequest(), 20L);

        assertEquals(2, result.getRecords().size());
        com.lawfirm.vo.CaseListVO own = result.getRecords().get(0);
        com.lawfirm.vo.CaseListVO unrelated = result.getRecords().get(1);
        assertTrue(own.getCanEdit());
        assertTrue(own.getCanDelete());
        assertFalse(own.getCanArchive());
        assertEquals(LocalDate.of(2026, 8, 10), own.getNextHearingDate());
        assertFalse(unrelated.getCanEdit());
        assertFalse(unrelated.getCanDelete());
        assertFalse(unrelated.getCanArchive());
    }

    @Test
    void batchCloseCannotBypassClosureWorkflow() {
        InvalidParameterException error = assertThrows(InvalidParameterException.class,
                () -> caseService.batchCloseCases(List.of(201L), 20L));

        assertTrue(error.getMessage().contains("结案申请"));
        verify(caseRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void batchArchiveAlwaysRequiresArchiveWorkflow() {
        assertThrows(InvalidParameterException.class,
                () -> caseService.batchArchiveCases(List.of(203L), "档案室A", 11L));
        verify(caseRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void batchArchiveCannotBypassArchiveWorkflow() {
        InvalidParameterException error = assertThrows(InvalidParameterException.class,
                () -> caseService.batchArchiveCases(List.of(205L), "二楼档案室", 11L));

        assertTrue(error.getMessage().contains("智能归档"));
        verify(caseRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void singleArchiveCannotBypassArchiveWorkflow() {
        InvalidParameterException error = assertThrows(InvalidParameterException.class,
                () -> caseService.archiveCase(206L, "档案室A"));

        assertTrue(error.getMessage().contains("智能归档"));
        verify(caseRepository, never()).save(org.mockito.ArgumentMatchers.any(Case.class));
    }

    @Test
    void caseListRejectsMalformedOrReversedFilingDateRange() {
        CaseQueryRequest malformed = new CaseQueryRequest();
        malformed.setStartDate("2026/07/01");
        assertThrows(InvalidParameterException.class, () -> caseService.getCaseList(malformed, 1L));

        CaseQueryRequest reversed = new CaseQueryRequest();
        reversed.setStartDate("2026-07-20");
        reversed.setEndDate("2026-07-01");
        assertThrows(InvalidParameterException.class, () -> caseService.getCaseList(reversed, 1L));
    }

    @Test
    void batchOperationRejectsMissingCaseIds() {
        Case existing = activeCase(204L, 20L);
        when(caseRepository.findAllById(List.of(204L, 999L))).thenReturn(List.of(existing));

        assertThrows(InvalidParameterException.class,
                () -> caseService.batchDeleteCases(List.of(204L, 999L), 20L));
        verify(caseRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    private Case activeCase(Long id, Long ownerId) {
        Case caseEntity = new Case();
        caseEntity.setId(id);
        caseEntity.setOwnerId(ownerId);
        caseEntity.setStatus("ACTIVE");
        caseEntity.setDeleted(false);
        return caseEntity;
    }

    private User user(Long id, String realName, String position, Long departmentId) {
        User user = new User();
        user.setId(id);
        user.setUsername(realName);
        user.setRealName(realName);
        user.setPosition(position);
        user.setDepartmentId(departmentId);
        user.setStatus(1);
        user.setDeleted(false);
        return user;
    }
}
