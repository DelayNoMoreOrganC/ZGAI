package com.lawfirm.service;

import com.lawfirm.entity.LawFirmLetterSequence;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.repository.LawFirmLetterSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LawFirmLetterSequenceService {
    private final LawFirmLetterSequenceRepository repository;

    @Transactional
    public synchronized int allocate(int year, String typeCode, Integer initialSerial, Long operatorId) {
        LawFirmLetterSequence sequence = repository.findForUpdate(year, typeCode).orElse(null);
        if (sequence == null) {
            if (initialSerial == null || initialSerial < 1 || initialSerial > 999999) {
                throw new InvalidParameterException("initialLetterSerial", "该年度和函种尚未编号，请填写1至999999之间的起始流水号");
            }
            sequence = new LawFirmLetterSequence();
            sequence.setLetterYear(year);
            sequence.setLetterTypeCode(typeCode);
            sequence.setLastSerial(initialSerial);
            sequence.setInitializedBy(operatorId);
            sequence.setInitializedAt(LocalDateTime.now());
            sequence.setUpdatedAt(LocalDateTime.now());
            repository.save(sequence);
            return initialSerial;
        }
        int next = sequence.getLastSerial() + 1;
        sequence.setLastSerial(next);
        sequence.setUpdatedAt(LocalDateTime.now());
        repository.save(sequence);
        return next;
    }

    @Transactional(readOnly = true)
    public boolean requiresInitialNumber(int year, String typeCode) {
        return repository.findByLetterYearAndLetterTypeCode(year, typeCode).isEmpty();
    }
}
