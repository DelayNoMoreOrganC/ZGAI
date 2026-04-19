package com.lawfirm.service;

import com.lawfirm.dto.MeetingRoomDTO;
import com.lawfirm.entity.MeetingRoom;
import com.lawfirm.repository.MeetingRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 会议室服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingRoomService {

    private final MeetingRoomRepository meetingRoomRepository;

    /**
     * 创建会议室
     */
    @Transactional
    public MeetingRoomDTO createMeetingRoom(MeetingRoomDTO dto) {
        MeetingRoom room = new MeetingRoom();
        BeanUtils.copyProperties(dto, room);
        room.setStatus(1);  // 默认启用

        room = meetingRoomRepository.save(room);

        return toDTO(room);
    }

    /**
     * 更新会议室
     */
    @Transactional
    public MeetingRoomDTO updateMeetingRoom(Long roomId, MeetingRoomDTO dto) {
        MeetingRoom room = meetingRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("会议室不存在"));

        room.setRoomName(dto.getRoomName());
        room.setLocation(dto.getLocation());
        room.setCapacity(dto.getCapacity());
        room.setFacilities(dto.getFacilities());
        if (dto.getStatus() != null) {
            room.setStatus(dto.getStatus());
        }

        room = meetingRoomRepository.save(room);

        return toDTO(room);
    }

    /**
     * 删除会议室
     */
    @Transactional
    public void deleteMeetingRoom(Long roomId) {
        MeetingRoom room = meetingRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("会议室不存在"));

        room.setDeleted(true);
        room.setStatus(0);  // 禁用
        meetingRoomRepository.save(room);
    }

    /**
     * 获取会议室列表
     */
    public List<MeetingRoomDTO> getMeetingRoomList(Boolean onlyEnabled) {
        List<MeetingRoom> rooms;

        if (onlyEnabled != null && onlyEnabled) {
            rooms = meetingRoomRepository.findByStatusEquals(1);
        } else {
            // 使用数据库查询优化，避免全表扫描
            rooms = new java.util.ArrayList<>();
            List<MeetingRoom> availableRooms = meetingRoomRepository.findByStatusOrderByIdAsc(1);
            List<MeetingRoom> otherRooms = meetingRoomRepository.findByStatusNotOrderByStatusAscIdAsc(1);
            rooms.addAll(availableRooms);
            rooms.addAll(otherRooms);
        }

        return rooms.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取会议室详情
     */
    public MeetingRoomDTO getMeetingRoomDetail(Long roomId) {
        MeetingRoom room = meetingRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("会议室不存在"));

        return toDTO(room);
    }

    /**
     * 启用/禁用会议室
     */
    @Transactional
    public void toggleMeetingRoomStatus(Long roomId, Integer status) {
        MeetingRoom room = meetingRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("会议室不存在"));

        room.setStatus(status);
        meetingRoomRepository.save(room);
    }

    // 辅助方法

    private MeetingRoomDTO toDTO(MeetingRoom room) {
        MeetingRoomDTO dto = new MeetingRoomDTO();
        BeanUtils.copyProperties(room, dto);
        dto.setStatusDesc(room.getStatus() == 1 ? "启用" : "禁用");
        return dto;
    }
}
