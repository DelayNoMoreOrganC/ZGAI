package com.lawfirm.repository;

import com.lawfirm.entity.ArchiveJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArchiveJobRepository extends JpaRepository<ArchiveJob, Long> {
    Optional<ArchiveJob> findByIdempotencyKeyAndDeletedFalse(String idempotencyKey);
    List<ArchiveJob> findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(Long caseId);
    List<ArchiveJob> findByStatusAndDeletedFalseOrderByCreatedAtAsc(String status);
    List<ArchiveJob> findByCreatedByAndDeletedFalseOrderByCreatedAtDesc(Long createdBy);
}
