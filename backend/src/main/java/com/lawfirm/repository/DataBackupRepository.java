package com.lawfirm.repository;

import com.lawfirm.entity.DataBackup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 数据备份仓库接口
 */
@Repository
public interface DataBackupRepository extends JpaRepository<DataBackup, Long> {

    /**
     * 查询指定日期之前的备份记录
     */
    @Query("SELECT b FROM DataBackup b WHERE b.backupTime < :beforeDate AND b.deleted = false")
    List<DataBackup> findBackupsBeforeDate(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * 查询成功的备份记录
     */
    @Query("SELECT b FROM DataBackup b WHERE b.backupStatus = 'SUCCESS' AND b.deleted = false ORDER BY b.backupTime DESC")
    List<DataBackup> findSuccessfulBackups();

    /**
     * 查询最近的N个备份记录
     */
    @Query("SELECT b FROM DataBackup b WHERE b.deleted = false ORDER BY b.backupTime DESC")
    List<DataBackup> findRecentBackups();

    /**
     * 根据备份类型查询
     */
    @Query("SELECT b FROM DataBackup b WHERE b.backupType = :backupType AND b.deleted = false ORDER BY b.backupTime DESC")
    List<DataBackup> findByBackupType(@Param("backupType") String backupType);

    /**
     * 统计指定时间范围内的备份次数
     */
    @Query("SELECT COUNT(b) FROM DataBackup b WHERE b.backupTime BETWEEN :startDate AND :endDate AND b.deleted = false")
    Long countBackupsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
}
