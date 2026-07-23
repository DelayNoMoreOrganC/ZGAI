package com.lawfirm.repository;

import com.lawfirm.entity.ArchiveOutput;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArchiveOutputRepository extends JpaRepository<ArchiveOutput, Long> {
    List<ArchiveOutput> findByCaseIdAndDeletedFalseOrderByVersionNoDesc(Long caseId);
    Optional<ArchiveOutput> findFirstByJobIdAndDeletedFalseOrderByVersionNoDesc(Long jobId);
}
