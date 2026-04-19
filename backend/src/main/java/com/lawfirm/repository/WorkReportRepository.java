package com.lawfirm.repository;

import com.lawfirm.entity.WorkReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作汇报Repository
 */
public interface WorkReportRepository extends JpaRepository<WorkReport, Long>, JpaSpecificationExecutor<WorkReport> {

    Page<WorkReport> findByReporterIdAndDeletedFalseOrderByReportDateDesc(Long reporterId, Pageable pageable);

    Page<WorkReport> findByStatusAndDeletedFalseOrderByReportDateDesc(String status, Pageable pageable);

    List<WorkReport> findByDepartmentAndDeletedFalseOrderByReportDateDesc(String department);

    List<WorkReport> findByReporterIdAndReportDateBetweenAndDeletedFalse(
            Long reporterId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}
