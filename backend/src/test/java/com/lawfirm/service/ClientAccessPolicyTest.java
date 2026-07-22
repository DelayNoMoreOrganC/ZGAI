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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientAccessPolicyTest {

    private ClientRepository clientRepository;
    private UserRepository userRepository;
    private ClientService service;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        userRepository = mock(UserRepository.class);
        service = new ClientService(
                clientRepository,
                mock(CaseRepository.class),
                userRepository,
                mock(CommunicationRecordRepository.class),
                mock(PartyRepository.class),
                mock(DepartmentRepository.class),
                mock(ConflictCheckRecordRepository.class));
    }

    @Test
    void namedGlobalViewerCanReadButCannotEditUnrelatedClient() {
        Client client = client(200L, 99L);
        User viewer = user(40L, "黄智明", "财务管理");
        when(clientRepository.findById(200L)).thenReturn(Optional.of(client));
        when(userRepository.findById(40L)).thenReturn(Optional.of(viewer));

        assertTrue(service.canAccessClient(200L, 40L));
        assertThrows(IllegalArgumentException.class, () -> service.assertClientEditable(200L, 40L));
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
}
