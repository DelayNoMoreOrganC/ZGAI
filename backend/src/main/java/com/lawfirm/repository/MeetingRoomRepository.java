package com.lawfirm.repository;

import com.lawfirm.entity.MeetingRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 会议室Repository
 */
@Repository
public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, Long> {

    /**
     * 根据状态查找会议室列表
     */
    List<MeetingRoom> findByStatusOrderByIdAsc(Integer status);

    /**
     * 查找所有启用的会议室
     */
    List<MeetingRoom> findByStatusEquals(Integer status);

    /**
     * 根据状态不等于指定值查找会议室（性能优化）
     */
    List<MeetingRoom> findByStatusNotOrderByStatusAscIdAsc(Integer status);
}
