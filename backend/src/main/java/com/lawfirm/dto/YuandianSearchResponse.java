package com.lawfirm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YuandianSearchResponse {

    private String provider;
    private String searchType;
    private String query;
    private LocalDateTime retrievedAt;
    private List<YuandianSearchResultDTO> results;
}
