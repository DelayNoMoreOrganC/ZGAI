package com.lawfirm.repository;

import com.lawfirm.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 考勤记录Repository
 */
@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long>,
        JpaSpecificationExecutor<AttendanceRecord> {

    /**
     * 查询用户在指定时间范围内的考勤记录
     */
    List<AttendanceRecord> findByUserIdAndStartDateBetweenAndDeletedFalse(
            Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询待审批的考勤记录
     */
    List<AttendanceRecord> findByApprovalStatusAndDeletedFalseOrderByCreatedAtDesc(
            String approvalStatus);

    /**
     * 统计用户在指定月份的请假天数
     */
    List<AttendanceRecord> findByUserIdAndAttendanceTypeAndStartDateBetweenAndDeletedFalse(
            Long userId, String attendanceType, LocalDate startDate, LocalDate endDate);
}
