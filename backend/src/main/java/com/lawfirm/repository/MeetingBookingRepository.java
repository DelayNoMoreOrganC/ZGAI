package com.lawfirm.repository;

import com.lawfirm.entity.MeetingBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议室预约Repository
 */
@Repository
public interface MeetingBookingRepository extends JpaRepository<MeetingBooking, Long> {

    /**
     * 根据会议室ID查找预约列表
     */
    List<MeetingBooking> findByRoomIdOrderByStartTimeDesc(Long roomId);

    /**
     * 根据预约人查找预约列表
     */
    List<MeetingBooking> findByBookerIdOrderByStartTimeDesc(Long bookerId);

    /**
     * 根据案件ID查找预约列表
     */
    List<MeetingBooking> findByCaseIdOrderByStartTimeDesc(Long caseId);

    /**
     * 检查时间段内是否有冲突的预约
     */
    @Query("SELECT b FROM MeetingBooking b WHERE b.roomId = :roomId " +
           "AND b.status != 'CANCELLED' " +
           "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<MeetingBooking> findConflictingBookings(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查找指定时间范围内的预约
     */
    @Query("SELECT b FROM MeetingBooking b WHERE b.roomId = :roomId " +
           "AND b.status != 'CANCELLED' " +
           "AND b.startTime >= :startTime AND b.endTime <= :endTime " +
           "ORDER BY b.startTime")
    List<MeetingBooking> findByRoomIdAndTimeRange(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
