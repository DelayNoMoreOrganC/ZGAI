package com.lawfirm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagEvaluationImportResult {
    private int rowCount;
    private int validCount;
    private int skippedCount;
    private int importedCount;
    private boolean canImport;

    @Builder.Default
    private List<RowResult> rows = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowResult {
        private int rowNumber;
        private String name;
        private String question;
        private String status;
        private String message;
    }
}
