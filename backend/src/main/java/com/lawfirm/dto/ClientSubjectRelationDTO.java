package com.lawfirm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClientSubjectRelationDTO {

    private Long id;
    private Long sourceClientId;
    private String sourceClientName;
    private Long targetClientId;
    private String targetSubjectName;
    private String targetCreditCode;
    private String relationType;
    private String relationTypeName;
    private String description;
    private String direction;
    private String relatedSubjectName;
    private String createdByName;
    private LocalDateTime createdAt;
}
