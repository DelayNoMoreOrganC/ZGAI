package com.lawfirm.dto;

import lombok.Data;

/**
 * 案件承办人员DTO
 */
@Data
public class CasePersonnelDTO {

    private Long id;
    private Long caseId;
    private String name;
    private String position;
    private String phone;
    private String court;
    private String department;
    private String remark;
}
