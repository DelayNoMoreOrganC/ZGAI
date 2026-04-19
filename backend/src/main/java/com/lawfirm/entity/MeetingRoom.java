package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会议室实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "meeting_room")
public class MeetingRoom extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "会议室名称不能为空")
    @Column(name = "room_name", nullable = false, length = 50)
    private String roomName;

    @Column
    private String location;

    @Column
    private Integer capacity;

    @Column
    private String facilities;

    @Column
    private Integer status = 1;
}
