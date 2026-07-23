package com.lawfirm.repository;

import com.lawfirm.entity.ArchiveJob;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ArchiveJobRepository extends JpaRepository<ArchiveJob, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from ArchiveJob job where job.id = :id and job.deleted = false")
    Optional<ArchiveJob> findActiveByIdForUpdate(@Param("id") Long id);

    Optional<ArchiveJob> findByIdempotencyKeyAndDeletedFalse(String idempotencyKey);
    List<ArchiveJob> findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(Long caseId);
    List<ArchiveJob> findByStatusAndDeletedFalseOrderByCreatedAtAsc(String status);
    List<ArchiveJob> findByCreatedByAndDeletedFalseOrderByCreatedAtDesc(Long createdBy);
}
