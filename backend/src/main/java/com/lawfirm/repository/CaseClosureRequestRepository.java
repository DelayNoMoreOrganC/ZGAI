package com.lawfirm.repository;

import com.lawfirm.entity.CaseClosureRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaseClosureRequestRepository extends JpaRepository<CaseClosureRequest, Long> {
    Optional<CaseClosureRequest> findByApprovalIdAndDeletedFalse(Long approvalId);
    Optional<CaseClosureRequest> findFirstByCaseIdAndDeletedFalseOrderByRequestedAtDesc(Long caseId);
    List<CaseClosureRequest> findByCaseIdAndStatusAndDeletedFalse(Long caseId, String status);
}
