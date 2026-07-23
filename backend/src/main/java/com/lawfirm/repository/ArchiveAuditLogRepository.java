package com.lawfirm.repository;

import com.lawfirm.entity.ArchiveAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchiveAuditLogRepository extends JpaRepository<ArchiveAuditLog, Long> {
    List<ArchiveAuditLog> findByJobIdOrderByCreatedAtAsc(Long jobId);
}
