package com.lawfirm.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class YuandianSearchResultDTO {

    private String importToken;
    private String resultType;
    private String externalId;
    private String parentId;
    private String title;
    private String referenceNo;
    private String content;
    private String authority;
    private String category;
    private String tags;
    private String validityStatus;
    private LocalDate date;
    private Double score;
    private String sourceReference;
    private Boolean ragEligible;
    private Map<String, String> metadata = new LinkedHashMap<>();
}
