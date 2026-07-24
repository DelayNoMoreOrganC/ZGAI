package com.lawfirm.event;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class SealApprovalDecisionEvent {
    Long approvalId;
    String status;
    Integer initialLetterSerial;
    Long operatorId;
    LocalDateTime decidedAt;
    String comments;
}
