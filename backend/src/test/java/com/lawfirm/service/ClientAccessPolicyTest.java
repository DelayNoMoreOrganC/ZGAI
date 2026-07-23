package com.lawfirm.service;

import com.lawfirm.entity.Client;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.Party;
import com.lawfirm.entity.User;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.ClientRepository;
import com.lawfirm.repository.CommunicationRecordRepository;
import com.lawfirm.repository.ConflictCheckRecordRepository;
import com.lawfirm.repository.DepartmentRepository;
import com.lawfirm.repository.PartyRepository;
import com.lawfirm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientAccessPolicyTest {

    private ClientRepository clientRepository;
    private CaseRepository caseRepository;
    private PartyRepository partyRepository;
    private com.lawfirm.repository.CaseMemberRepository caseMemberRepository;
    private CommunicationRecordRepository communicationRecordRepository;
    private UserRepository userRepository;
    private UserPermissionService userPermissionService;
    private ClientService service;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        caseRepository = mock(CaseRepository.class);
        partyRepository = mock(PartyRepository.class);
        caseMemberRepository = mock(com.lawfirm.repository.CaseMemberRepository.class);
        communicationRecordRepository = mock(CommunicationRecordRepository.class);
        userRepository = mock(UserRepository.class);
        userPermissionService = mock(UserPermissionService.class);
        service = new ClientService(
                clientRepository,
                caseRepository,
                userRepository,
                communicationRecordRepository,
                partyRepository,
                caseMemberRepository,
                mock(DepartmentRepository.class),
                mock(ConflictCheckRecordRepository.class),
                userPermissionService,
                mock(ConflictWaiverAttachmentService.class),
                mock(ClientSubjectRelationService.class));
    }

    @Test
    void permissionedGlobalViewerCanReadButCannotEditUnrelatedClient() {
        Client client = client(200L, 99L);
        User viewer = user(40L, "黄智明", "财务管理");
        when(clientRepository.findById(200L)).thenReturn(Optional.of(client));
        when(userRepository.findById(40L)).thenReturn(Optional.of(viewer));
        when(userPermissionService.hasPermission(viewer, "CLIENT_VIEW_ALL")).thenReturn(true);

        assertTrue(service.canAccessClient(200L, 40L));
        assertThrows(AccessDeniedException.class, () -> service.assertClientEditable(200L, 40L));
    }

    @Test
    void clientOwnerAndDirectorCanEdit() {
        Client client = client(201L, 41L);
        User owner = user(41L, "律师甲", "律师");
        User director = user(42L, "主任甲", "主任");
        when(clientRepository.findById(201L)).thenReturn(Optional.of(client));
        when(userRepository.findById(41L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(42L)).thenReturn(Optional.of(director));

        assertDoesNotThrow(() -> service.assertClientEditable(201L, 41L));
        assertDoesNotThrow(() -> service.assertClientEditable(201L, 42L));
    }

    @Test
    void departmentMemberCanViewColleaguesClientButCannotEditIt() {
        Client client = client(202L, 51L);
        User colleague = user(51L, "律师甲", "律师");
        colleague.setDepartmentId(6L);
        User departmentMember = user(52L, "律师乙", "律师");
        departmentMember.setDepartmentId(6L);
        when(clientRepository.findById(202L)).thenReturn(Optional.of(client));
        when(userRepository.findById(51L)).thenReturn(Optional.of(colleague));
        when(userRepository.findById(52L)).thenReturn(Optional.of(departmentMember));

        assertTrue(service.canAccessClient(202L, 52L));
        assertThrows(AccessDeniedException.class, () -> service.assertClientEditable(202L, 52L));
    }

    @Test
    void clientDetailReturnsEffectiveActionPermissions() {
        Client client = client(205L, 81L);
        client.setClientName("乙公司");
        User owner = user(81L, "律师乙", "律师");
        when(clientRepository.findById(205L)).thenReturn(Optional.of(client));
        when(userRepository.findById(81L)).thenReturn(Optional.of(owner));
        when(partyRepository.findByNameAndDeletedFalse("乙公司")).thenReturn(List.of());
        when(communicationRecordRepository.findByClientIdOrderByCommunicationDateDesc(205L)).thenReturn(List.of());
        when(userPermissionService.hasPermission(owner, "CLIENT_EDIT")).thenReturn(true);
        when(userPermissionService.hasPermission(owner, "CLIENT_DELETE")).thenReturn(false);

        com.lawfirm.dto.ClientDTO detail = service.getClientById(205L, 81L);

        assertTrue(detail.getCanEdit());
        assertFalse(detail.getCanDelete());
    }

    @Test
    void unrelatedDepartmentCannotViewClient() {
        Client client = client(203L, 61L);
        User owner = user(61L, "律师丙", "律师");
        owner.setDepartmentId(7L);
        User outsider = user(62L, "律师丁", "律师");
        outsider.setDepartmentId(8L);
        when(clientRepository.findById(203L)).thenReturn(Optional.of(client));
        when(userRepository.findById(61L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(62L)).thenReturn(Optional.of(outsider));

        org.junit.jupiter.api.Assertions.assertFalse(service.canAccessClient(203L, 62L));
        assertThrows(AccessDeniedException.class, () -> service.getClientById(203L, 62L));
    }

    @Test
    void clientDetailOnlyReturnsCasesVisibleToCurrentUser() {
        Client client = client(204L, 71L);
        client.setClientName("甲公司");
        User lawyer = user(71L, "律师甲", "律师");
        Case ownCase = activeCase(301L, 71L, "本部门案件");
        Case unrelatedCase = activeCase(302L, 99L, "其他部门案件");
        Party ownParty = party(301L, "甲公司");
        Party unrelatedParty = party(302L, "甲公司");

        when(clientRepository.findById(204L)).thenReturn(Optional.of(client));
        when(userRepository.findById(71L)).thenReturn(Optional.of(lawyer));
        when(partyRepository.findByNameAndDeletedFalse("甲公司")).thenReturn(List.of(ownParty, unrelatedParty));
        when(caseRepository.findById(301L)).thenReturn(Optional.of(ownCase));
        when(caseRepository.findById(302L)).thenReturn(Optional.of(unrelatedCase));
        when(caseMemberRepository.findByCaseIdAndDeletedFalse(302L)).thenReturn(List.of());

        List<com.lawfirm.vo.CaseListVO> result = service.getClientCases(204L, 71L);

        assertEquals(1, result.size());
        assertEquals(301L, result.get(0).getId());
    }

    private Client client(Long id, Long ownerId) {
        Client client = new Client();
        client.setId(id);
        client.setOwnerId(ownerId);
        client.setClientOwnerIds(String.valueOf(ownerId));
        client.setDeleted(false);
        return client;
    }

    private User user(Long id, String realName, String position) {
        User user = new User();
        user.setId(id);
        user.setUsername(realName);
        user.setRealName(realName);
        user.setPosition(position);
        user.setDeleted(false);
        return user;
    }

    private Case activeCase(Long id, Long ownerId, String name) {
        Case caseEntity = new Case();
        caseEntity.setId(id);
        caseEntity.setOwnerId(ownerId);
        caseEntity.setCaseName(name);
        caseEntity.setStatus("ACTIVE");
        caseEntity.setDeleted(false);
        return caseEntity;
    }

    private Party party(Long caseId, String name) {
        Party party = new Party();
        party.setCaseId(caseId);
        party.setName(name);
        party.setDeleted(false);
        return party;
    }
}
