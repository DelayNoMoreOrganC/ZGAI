package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.User;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
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

    @InjectMocks private CaseService caseService;

    @Test
    void administrativeUserCanViewAllCasesButCannotEditThem() {
        Case caseEntity = activeCase(100L, 20L);
        User administrative = user(10L, "行政甲", "行政管理1", 9L);
        when(caseRepository.findById(100L)).thenReturn(Optional.of(caseEntity));
        when(userRepository.findById(10L)).thenReturn(Optional.of(administrative));

        assertTrue(caseService.canAccessCase(100L, 10L));
        assertThrows(InvalidParameterException.class, () -> caseService.assertCaseEditable(100L, 10L));
    }

    @Test
    void directorAndFilingAdministratorCanEditBeforeClosure() {
        Case caseEntity = activeCase(101L, 20L);
        User director = user(11L, "主任甲", "主任", 1L);
        User filingAdministrator = user(12L, "田颖思", "行政管理1", 9L);
        when(caseRepository.findById(101L)).thenReturn(Optional.of(caseEntity));
        when(userRepository.findById(11L)).thenReturn(Optional.of(director));
        when(userRepository.findById(12L)).thenReturn(Optional.of(filingAdministrator));

        assertDoesNotThrow(() -> caseService.assertCaseEditable(101L, 11L));
        assertDoesNotThrow(() -> caseService.assertCaseEditable(101L, 12L));
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
        assertThrows(InvalidParameterException.class, () -> caseService.assertCaseEditable(102L, 21L));
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
    void batchCloseRejectsUnrelatedCaseBeforeSavingAnything() {
        Case ownCase = activeCase(201L, 20L);
        ownCase.setCaseName("本人案件");
        Case unrelatedCase = activeCase(202L, 30L);
        unrelatedCase.setCaseName("无关案件");
        User lawyer = user(20L, "律师甲", "律师", 2L);

        when(caseRepository.findAllById(List.of(201L, 202L))).thenReturn(List.of(ownCase, unrelatedCase));
        when(caseRepository.findById(201L)).thenReturn(Optional.of(ownCase));
        when(caseRepository.findById(202L)).thenReturn(Optional.of(unrelatedCase));
        when(userRepository.findById(20L)).thenReturn(Optional.of(lawyer));
        when(caseMemberRepository.findByCaseIdAndDeletedFalse(202L)).thenReturn(Collections.emptyList());

        assertThrows(InvalidParameterException.class,
                () -> caseService.batchCloseCases(List.of(201L, 202L), 20L));
        verify(caseRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void batchArchiveRequiresClosedCase() {
        Case active = activeCase(203L, 20L);
        active.setCaseName("尚未结案");
        User director = user(11L, "主任甲", "主任", 1L);
        when(caseRepository.findAllById(List.of(203L))).thenReturn(List.of(active));
        when(caseRepository.findById(203L)).thenReturn(Optional.of(active));
        when(userRepository.findById(11L)).thenReturn(Optional.of(director));

        assertThrows(InvalidParameterException.class,
                () -> caseService.batchArchiveCases(List.of(203L), 11L));
        verify(caseRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
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
