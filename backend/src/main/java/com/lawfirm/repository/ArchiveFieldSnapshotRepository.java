package com.lawfirm.repository;

import com.lawfirm.entity.ArchiveFieldSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArchiveFieldSnapshotRepository extends JpaRepository<ArchiveFieldSnapshot, Long> {
    List<ArchiveFieldSnapshot> findByJobIdAndDeletedFalseOrderByIdAsc(Long jobId);
    Optional<ArchiveFieldSnapshot> findByJobIdAndFieldKeyAndDeletedFalse(Long jobId, String fieldKey);
}
