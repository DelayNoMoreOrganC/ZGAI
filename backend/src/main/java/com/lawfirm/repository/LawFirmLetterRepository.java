package com.lawfirm.repository;

import com.lawfirm.entity.LawFirmLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LawFirmLetterRepository extends JpaRepository<LawFirmLetter, Long> {
    List<LawFirmLetter> findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(Long caseId);
    Optional<LawFirmLetter> findByIdAndDeletedFalse(Long id);
    Optional<LawFirmLetter> findByApprovalIdAndDeletedFalse(Long approvalId);
}
