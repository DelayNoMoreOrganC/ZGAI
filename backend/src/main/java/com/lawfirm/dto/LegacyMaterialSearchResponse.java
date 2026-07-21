package com.lawfirm.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 旧系统资料检索响应。
 */
@Data
public class LegacyMaterialSearchResponse {

    private Long recordId;

    private Boolean archivePathConfigured = false;

    private String archiveRootPath;

    private String message;

    private Integer total = 0;

    private List<LegacyMaterialSearchResultDTO> results = new ArrayList<>();
}
