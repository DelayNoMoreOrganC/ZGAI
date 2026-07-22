package com.lawfirm.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Administrative audit view. Request parameters and internal errors are intentionally excluded.
 */
@Data
public class AuditLogDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String module;
    private String operation;
    private String method;
    private String ip;
    private Integer status;
    private Integer executionTime;
    private LocalDateTime createdAt;
}
