package com.lawfirm.service;

import com.lawfirm.dto.MeetingBookingCreateRequest;
import com.lawfirm.dto.MeetingBookingDTO;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.MeetingBooking;
import com.lawfirm.entity.MeetingRoom;
import com.lawfirm.repository.CalendarRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.MeetingBookingRepository;
import com.lawfirm.repository.MeetingRoomRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会议室预约服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingBookingService {

    private final MeetingBookingRepository meetingBookingRepository;
    private final MeetingRoomRepository meetingRoomRepository;
    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final CalendarRepository calendarRepository;
    private final NotificationService notificationService;

    /**
     * 创建预约
     */
    @Transactional
    public MeetingBookingDTO createBooking(MeetingBookingCreateRequest request, Long bookerId) {
        // 验证会议室
        MeetingRoom room = meetingRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("会议室不存在"));

        if (room.getStatus() != 1) {
            throw new RuntimeException("会议室已禁用，无法预约");
        }

        // 验证时间
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new RuntimeException("开始时间不能晚于结束时间");
        }

        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("不能预约过去的时间");
        }

        // 检查冲突
        List<MeetingBooking> conflicts = meetingBookingRepository.findConflictingBookings(
                request.getRoomId(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (!conflicts.isEmpty()) {
            throw new RuntimeException("该时间段已有预约，请选择其他时间");
        }

        // 创建预约
        MeetingBooking booking = new MeetingBooking();
        BeanUtils.copyProperties(request, booking);
        booking.setBookerId(bookerId);
        booking.setStatus("CONFIRMED");

        booking = meetingBookingRepository.save(booking);

        // 发送通知
        notificationService.sendNotification(
                booking.getBookerId(),
                "会议室预约成功",
                "您已成功预约" + room.getRoomName() + "，时间：" +
                        booking.getStartTime() + " 至 " + booking.getEndTime(),
                NotificationService.CATEGORY_CALENDAR,
                booking.getId(),
                "MEETING_BOOKING"
        );

        return toDTO(booking);
    }

    /**
     * 取消预约
     */
    @Transactional
    public void cancelBooking(Long bookingId, Long userId) {
        MeetingBooking booking = meetingBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("预约记录不存在"));

        // 验证权限
        if (!booking.getBookerId().equals(userId)) {
            throw new RuntimeException("只能取消自己的预约");
        }

        booking.setStatus("CANCELLED");
        meetingBookingRepository.save(booking);
    }

    /**
     * 获取预约列表
     */
    public List<MeetingBookingDTO> getBookingList(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        List<MeetingBooking> bookings;

        if (roomId != null && startTime != null && endTime != null) {
            bookings = meetingBookingRepository.findByRoomIdAndTimeRange(roomId, startTime, endTime);
        } else if (roomId != null) {
            bookings = meetingBookingRepository.findByRoomIdOrderByStartTimeDesc(roomId);
        } else {
            bookings = meetingBookingRepository.findAll();
        }

        return bookings.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取我的预约列表
     */
    public List<MeetingBookingDTO> getMyBookings(Long bookerId) {
        List<MeetingBooking> bookings = meetingBookingRepository.findByBookerIdOrderByStartTimeDesc(bookerId);

        return bookings.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取预约详情
     */
    public MeetingBookingDTO getBookingDetail(Long bookingId) {
        MeetingBooking booking = meetingBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("预约记录不存在"));

        return toDTO(booking);
    }

    /**
     * 检查预约冲突
     */
    public boolean checkConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        List<MeetingBooking> conflicts = meetingBookingRepository.findConflictingBookings(
                roomId, startTime, endTime
        );

        return !conflicts.isEmpty();
    }

    /**
     * 关联开庭日程
     */
    @Transactional
    public void linkToHearing(Long bookingId, Long caseId) {
        MeetingBooking booking = meetingBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("预约记录不存在"));

        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("案件不存在"));

        booking.setCaseId(caseId);
        meetingBookingRepository.save(booking);

        // 这里可以添加更多逻辑，比如同步到Calendar
        log.info("会议室预约 {} 已关联案件 {}", bookingId, caseId);
    }

    // 辅助方法

    private MeetingBookingDTO toDTO(MeetingBooking booking) {
        MeetingBookingDTO dto = new MeetingBookingDTO();
        BeanUtils.copyProperties(booking, dto);

        // 设置会议室名称
        meetingRoomRepository.findById(booking.getRoomId())
                .ifPresent(room -> dto.setRoomName(room.getRoomName()));

        // 设置预约人姓名
        userRepository.findById(booking.getBookerId())
                .ifPresent(user -> dto.setBookerName(user.getRealName()));

        // 设置案件名称
        if (booking.getCaseId() != null) {
            caseRepository.findById(booking.getCaseId())
                    .ifPresent(caseEntity -> dto.setCaseName(caseEntity.getCaseName()));
        }

        // 设置状态描述
        dto.setStatusDesc(getStatusDesc(booking.getStatus()));

        return dto;
    }

    private String getStatusDesc(String status) {
        if (status == null) return null;
        switch (status) {
            case "CONFIRMED": return "已确认";
            case "CANCELLED": return "已取消";
            case "COMPLETED": return "已完成";
            default: return status;
        }
    }
}
