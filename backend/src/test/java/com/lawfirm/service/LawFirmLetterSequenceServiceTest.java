package com.lawfirm.service;

import com.lawfirm.entity.LawFirmLetterSequence;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.repository.LawFirmLetterSequenceRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class LawFirmLetterSequenceServiceTest {
    private final LawFirmLetterSequenceRepository repository = mock(LawFirmLetterSequenceRepository.class);
    private final LawFirmLetterSequenceService service = new LawFirmLetterSequenceService(repository);

    @Test
    void firstNumberMustBeSpecifiedByAdministrativeApprover() {
        when(repository.findForUpdate(2026, "民")).thenReturn(Optional.empty());
        assertThrows(InvalidParameterException.class, () -> service.allocate(2026, "民", null, 9L));
        verify(repository, never()).save(any());
    }

    @Test
    void firstNumberUsesAdministrativeValueAndFollowingNumberIncrements() {
        when(repository.findForUpdate(2026, "民")).thenReturn(Optional.empty());
        assertThat(service.allocate(2026, "民", 25, 9L)).isEqualTo(25);
        verify(repository).save(argThat(item -> item.getLastSerial() == 25 && item.getInitializedBy() == 9L));

        LawFirmLetterSequence existing = new LawFirmLetterSequence();
        existing.setLastSerial(25);
        when(repository.findForUpdate(2026, "民")).thenReturn(Optional.of(existing));
        assertThat(service.allocate(2026, "民", null, 9L)).isEqualTo(26);
        assertThat(existing.getLastSerial()).isEqualTo(26);
    }
}
