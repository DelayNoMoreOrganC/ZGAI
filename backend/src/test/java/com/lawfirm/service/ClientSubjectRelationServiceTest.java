package com.lawfirm.service;

import com.lawfirm.dto.ClientSubjectRelationDTO;
import com.lawfirm.entity.Client;
import com.lawfirm.entity.ClientSubjectRelation;
import com.lawfirm.repository.ClientRepository;
import com.lawfirm.repository.ClientSubjectRelationRepository;
import com.lawfirm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientSubjectRelationServiceTest {

    private ClientSubjectRelationRepository relationRepository;
    private ClientRepository clientRepository;
    private ClientSubjectRelationService service;

    @BeforeEach
    void setUp() {
        relationRepository = mock(ClientSubjectRelationRepository.class);
        clientRepository = mock(ClientRepository.class);
        service = new ClientSubjectRelationService(
                relationRepository, clientRepository, mock(UserRepository.class));
        when(relationRepository.findBySourceClientIdAndDeletedFalseOrderByCreatedAtDesc(any()))
                .thenReturn(Collections.emptyList());
        when(relationRepository.save(any(ClientSubjectRelation.class))).thenAnswer(invocation -> {
            ClientSubjectRelation relation = invocation.getArgument(0);
            relation.setId(relation.getId() == null ? 71L : relation.getId());
            relation.setCreatedAt(LocalDateTime.of(2026, 7, 23, 19, 0));
            return relation;
        });
    }

    @Test
    void createsRelationToExistingVisibleClient() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client(1L, "示例集团", "GROUP001")));
        when(clientRepository.findById(2L)).thenReturn(Optional.of(client(2L, "示例项目公司", "PROJECT002")));
        ClientSubjectRelationDTO request = new ClientSubjectRelationDTO();
        request.setRelationType("SUBSIDIARY");
        request.setTargetClientId(2L);
        request.setDescription("集团持股并实际控制");

        ClientSubjectRelationDTO result = service.create(1L, request, 8L);

        assertEquals("示例项目公司", result.getTargetSubjectName());
        assertEquals("PROJECT002", result.getTargetCreditCode());
        assertEquals("子公司", result.getRelationTypeName());
        assertEquals("OUTBOUND", result.getDirection());
    }

    @Test
    void rejectsDuplicateManualRelationAfterNameNormalization() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client(1L, "示例集团", "GROUP001")));
        ClientSubjectRelation existing = new ClientSubjectRelation();
        existing.setId(70L);
        existing.setSourceClientId(1L);
        existing.setTargetSubjectName("佛山示例有限公司");
        existing.setRelationType("AFFILIATE");
        when(relationRepository.findBySourceClientIdAndDeletedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.singletonList(existing));
        ClientSubjectRelationDTO request = new ClientSubjectRelationDTO();
        request.setRelationType("AFFILIATE");
        request.setTargetSubjectName("佛山示例公司");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.create(1L, request, 8L));

        assertTrue(error.getMessage().contains("已存在"));
    }

    @Test
    void onlySourceClientCanDeleteRelation() {
        ClientSubjectRelation relation = new ClientSubjectRelation();
        relation.setId(70L);
        relation.setSourceClientId(1L);
        relation.setTargetClientId(2L);
        relation.setDeleted(false);
        when(relationRepository.findById(70L)).thenReturn(Optional.of(relation));

        assertThrows(IllegalArgumentException.class, () -> service.delete(2L, 70L));
        service.delete(1L, 70L);

        assertTrue(relation.getDeleted());
    }

    private Client client(Long id, String name, String creditCode) {
        Client client = new Client();
        client.setId(id);
        client.setClientName(name);
        client.setClientType("企业");
        client.setCreditCode(creditCode);
        client.setDeleted(false);
        return client;
    }
}
