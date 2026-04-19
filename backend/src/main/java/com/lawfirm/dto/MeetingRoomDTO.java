package com.lawfirm.dto;

import lombok.Data;

/**
 * 会议室DTO
 */
@Data
public class MeetingRoomDTO {
    private Long id;
    private String roomName;
    private String location;
    private Integer capacity;
    private String facilities;
    private Integer status;
    private String statusDesc;
}
