package com.lawfirm.repository;

import com.lawfirm.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作审计日志Repository
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 根据用户ID查找日志
     */
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 根据模块查找日志
     */
    List<AuditLog> findByModuleOrderByCreatedAtDesc(String module);

    /**
     * 根据时间范围查找日志
     */
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :start AND :end")
    Page<AuditLog> findByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    /**
     * 统计各模块的操作次数
     */
    @Query("SELECT a.module, a.operation, COUNT(a) FROM AuditLog a WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.module, a.operation ORDER BY a.module")
    List<Object[]> getModuleStatistics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计各模块的总操作次数
     */
    @Query("SELECT a.module, COUNT(a) FROM AuditLog a WHERE a.createdAt BETWEEN :start AND :end GROUP BY a.module ORDER BY COUNT(a) DESC")
    List<Object[]> getModuleTotalStatistics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
