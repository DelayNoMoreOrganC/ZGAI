package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.LegacyMaterialSearchRequest;
import com.lawfirm.dto.LegacyMaterialSearchResponse;
import com.lawfirm.dto.LegacyMaterialSearchResultDTO;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.LegacyMaterialSearchRecord;
import com.lawfirm.entity.LegacyMaterialSearchResult;
import com.lawfirm.entity.Party;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.ClientRepository;
import com.lawfirm.repository.DepartmentRepository;
import com.lawfirm.repository.LegacyMaterialSearchRecordRepository;
import com.lawfirm.repository.LegacyMaterialSearchResultRepository;
import com.lawfirm.repository.PartyRepository;
import com.lawfirm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacyMaterialSearchServiceTest {

    private CaseRepository caseRepository;
    private ClientRepository clientRepository;
    private UserRepository userRepository;
    private DepartmentRepository departmentRepository;
    private PartyRepository partyRepository;
    private LegacyMaterialSearchRecordRepository recordRepository;
    private LegacyMaterialSearchResultRepository resultRepository;
    private CaseService caseService;
    private LegacyMaterialSearchService service;

    @BeforeEach
    void setUp() {
        caseRepository = mock(CaseRepository.class);
        clientRepository = mock(ClientRepository.class);
        userRepository = mock(UserRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        partyRepository = mock(PartyRepository.class);
        recordRepository = mock(LegacyMaterialSearchRecordRepository.class);
        resultRepository = mock(LegacyMaterialSearchResultRepository.class);
        caseService = mock(CaseService.class);
        service = new LegacyMaterialSearchService(
                caseRepository,
                clientRepository,
                userRepository,
                departmentRepository,
                partyRepository,
                recordRepository,
                resultRepository,
                caseService,
                new ObjectMapper());
        ReflectionTestUtils.setField(service, "legacyArchiveRootPath", "");
        ReflectionTestUtils.setField(service, "maxScanResults", 30);
        ReflectionTestUtils.setField(service, "maxScanFiles", 1000);
    }

    @Test
    void requiresAVisibleSourceCase() {
        LegacyMaterialSearchRequest request = new LegacyMaterialSearchRequest();

        assertThrows(InvalidParameterException.class, () -> service.search(request, 7L));
    }

    @Test
    void derivesStrongElementsAndDoesNotExposeStoragePaths(@TempDir Path archiveRoot) throws Exception {
        Case sourceCase = sourceCase();
        Party party = new Party();
        party.setName("吴炜涛");
        party.setDeleted(false);
        party.setCaseId(10L);
        party.setPartyType("INDIVIDUAL");
        party.setPartyRole("DEFENDANT");

        Path folder = Files.createDirectories(archiveRoot.resolve("2024").resolve("ZGAI-2026-001_吴炜涛"));
        Files.write(folder.resolve("起诉状.pdf"), new byte[]{1, 2, 3});

        when(caseRepository.findById(10L)).thenReturn(Optional.of(sourceCase));
        when(partyRepository.findByCaseIdAndDeletedFalse(10L)).thenReturn(Collections.singletonList(party));
        AtomicLong ids = new AtomicLong(1);
        when(recordRepository.save(any(LegacyMaterialSearchRecord.class))).thenAnswer(invocation -> {
            LegacyMaterialSearchRecord record = invocation.getArgument(0);
            if (record.getId() == null) record.setId(ids.getAndIncrement());
            return record;
        });
        when(resultRepository.save(any(LegacyMaterialSearchResult.class))).thenAnswer(invocation -> {
            LegacyMaterialSearchResult result = invocation.getArgument(0);
            result.setId(ids.getAndIncrement());
            return result;
        });
        ReflectionTestUtils.setField(service, "legacyArchiveRootPath", archiveRoot.toString());

        LegacyMaterialSearchRequest request = new LegacyMaterialSearchRequest();
        request.setCaseId(10L);
        request.setLimit(30);
        LegacyMaterialSearchResponse response = service.search(request, 7L);

        verify(caseService).assertCaseVisible(10L, 7L);
        assertEquals(1, response.getTotal());
        assertTrue(response.getSearchElements().stream().anyMatch(value -> value.contains("ZGAI案号")));
        assertTrue(response.getSearchElements().stream().anyMatch(value -> value.contains("当事人")));
        LegacyMaterialSearchResultDTO result = response.getResults().get(0);
        assertEquals("起诉状.pdf", result.getTitle());
        assertTrue(result.getDownloadable());
        assertFalse(hasField(LegacyMaterialSearchResponse.class, "archiveRootPath"));
        assertFalse(hasField(LegacyMaterialSearchResultDTO.class, "materialPath"));
    }

    @Test
    void downloadRechecksCaseAccessAndRejectsTraversal(@TempDir Path archiveRoot) {
        LegacyMaterialSearchResult stored = new LegacyMaterialSearchResult();
        stored.setId(3L);
        stored.setSourceCaseId(10L);
        stored.setRelativePath("../outside.pdf");
        stored.setFileName("outside.pdf");
        when(resultRepository.findById(3L)).thenReturn(Optional.of(stored));
        ReflectionTestUtils.setField(service, "legacyArchiveRootPath", archiveRoot.toString());

        assertThrows(InvalidParameterException.class, () -> service.loadDownload(3L, 7L));
        verify(caseService).assertCaseVisible(10L, 7L);
    }

    private Case sourceCase() {
        Case sourceCase = new Case();
        sourceCase.setId(10L);
        sourceCase.setCaseNumber("ZGAI-2026-001");
        sourceCase.setCaseName("逯景诉吴炜涛合同纠纷");
        sourceCase.setCaseReason("合同纠纷");
        sourceCase.setDeleted(false);
        return sourceCase;
    }

    private boolean hasField(Class<?> type, String name) {
        try {
            type.getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException ignored) {
            return false;
        }
    }
}
