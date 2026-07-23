package com.lawfirm.service;

import com.lawfirm.entity.Client;
import com.lawfirm.entity.User;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.ClientRepository;
import com.lawfirm.repository.CommunicationRecordRepository;
import com.lawfirm.repository.ConflictCheckRecordRepository;
import com.lawfirm.repository.DepartmentRepository;
import com.lawfirm.repository.PartyRepository;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.util.PageResult;
import com.lawfirm.dto.ClientDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientListServiceTest {

    private ClientRepository clientRepository;
    private UserRepository userRepository;
    private PartyRepository partyRepository;
    private CommunicationRecordRepository communicationRecordRepository;
    private ClientService service;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        userRepository = mock(UserRepository.class);
        partyRepository = mock(PartyRepository.class);
        communicationRecordRepository = mock(CommunicationRecordRepository.class);
        service = new ClientService(
                clientRepository,
                mock(CaseRepository.class),
                userRepository,
                communicationRecordRepository,
                partyRepository,
                mock(com.lawfirm.repository.CaseMemberRepository.class),
                mock(DepartmentRepository.class),
                mock(ConflictCheckRecordRepository.class),
                mock(UserPermissionService.class),
                mock(ConflictWaiverAttachmentService.class),
                mock(ClientSubjectRelationService.class));

        when(partyRepository.findByNameAndDeletedFalse(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of());
        when(communicationRecordRepository.findByClientIdOrderByCommunicationDateDesc(anyLong()))
                .thenReturn(List.of());
    }

    @Test
    void appliesFiltersBeforePaginationAndReturnsFilteredTotal() {
        User admin = user(1L, "admin", "开发管理员");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(clientRepository.findByDeletedFalse()).thenReturn(List.of(
                client(11L, "甲公司", "企业", 3L, LocalDateTime.of(2026, 7, 20, 9, 0)),
                client(12L, "乙公司", "企业", 3L, LocalDateTime.of(2026, 7, 21, 9, 0)),
                client(13L, "丙公司", "企业", 4L, LocalDateTime.of(2026, 7, 22, 9, 0)),
                client(14L, "张三", "个人", 3L, LocalDateTime.of(2026, 7, 23, 9, 0))));

        PageResult<ClientDTO> result = service.getClients(1, 1, 1L, null, "企业", 3L);

        assertEquals(2L, result.getTotal());
        assertEquals(1L, result.getPage());
        assertEquals(1, result.getRecords().size());
        assertEquals(11L, result.getRecords().get(0).getId());
    }

    @Test
    void keywordCanMatchSourceOrOwnerNameWithinVisibleClients() {
        User lawyer = user(20L, "lawyer-a", "陈律师");
        Client client = client(21L, "某科技公司", "企业", 2L, LocalDateTime.of(2026, 7, 23, 9, 0));
        client.setSourceUserIds("20");
        when(userRepository.findById(20L)).thenReturn(Optional.of(lawyer));
        when(clientRepository.findByDeletedFalse()).thenReturn(List.of(client));

        PageResult<ClientDTO> result = service.getClients(0, 20, 20L, "陈律师", null, null);

        assertEquals(1L, result.getTotal());
        assertEquals(21L, result.getRecords().get(0).getId());
        assertEquals("陈律师", result.getRecords().get(0).getSourceUserNames());
    }

    private Client client(Long id, String name, String type, Long departmentId, LocalDateTime createdAt) {
        Client client = new Client();
        client.setId(id);
        client.setClientName(name);
        client.setClientType(type);
        client.setDepartmentId(departmentId);
        client.setCreatedAt(createdAt);
        client.setDeleted(false);
        return client;
    }

    private User user(Long id, String username, String realName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(realName);
        user.setDeleted(false);
        user.setStatus(1);
        return user;
    }
}
