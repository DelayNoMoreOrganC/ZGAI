package com.lawfirm.migration;

import java.util.ArrayList;
import java.util.List;

public class MigrationReport {

    private String migrationId;
    private String action;
    private String status;
    private String sourceProduct;
    private String targetProduct;
    private String targetDatabase;
    private String sourceSha256;
    private String startedAt;
    private String completedAt;
    private String errorMessage;
    private long sourceRowTotal;
    private long targetRowTotal;
    private final List<TableResult> tables = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public String getMigrationId() { return migrationId; }
    public void setMigrationId(String migrationId) { this.migrationId = migrationId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSourceProduct() { return sourceProduct; }
    public void setSourceProduct(String sourceProduct) { this.sourceProduct = sourceProduct; }
    public String getTargetProduct() { return targetProduct; }
    public void setTargetProduct(String targetProduct) { this.targetProduct = targetProduct; }
    public String getTargetDatabase() { return targetDatabase; }
    public void setTargetDatabase(String targetDatabase) { this.targetDatabase = targetDatabase; }
    public String getSourceSha256() { return sourceSha256; }
    public void setSourceSha256(String sourceSha256) { this.sourceSha256 = sourceSha256; }
    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }
    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public long getSourceRowTotal() { return sourceRowTotal; }
    public void setSourceRowTotal(long sourceRowTotal) { this.sourceRowTotal = sourceRowTotal; }
    public long getTargetRowTotal() { return targetRowTotal; }
    public void setTargetRowTotal(long targetRowTotal) { this.targetRowTotal = targetRowTotal; }
    public List<TableResult> getTables() { return tables; }
    public List<String> getWarnings() { return warnings; }

    public static class TableResult {
        private String sourceTable;
        private String targetTable;
        private long sourceRows;
        private long targetRows;
        private List<String> primaryKeyColumns = new ArrayList<>();
        private String sourcePrimaryKeySha256;
        private String targetPrimaryKeySha256;
        private String status;

        public String getSourceTable() { return sourceTable; }
        public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
        public String getTargetTable() { return targetTable; }
        public void setTargetTable(String targetTable) { this.targetTable = targetTable; }
        public long getSourceRows() { return sourceRows; }
        public void setSourceRows(long sourceRows) { this.sourceRows = sourceRows; }
        public long getTargetRows() { return targetRows; }
        public void setTargetRows(long targetRows) { this.targetRows = targetRows; }
        public List<String> getPrimaryKeyColumns() { return primaryKeyColumns; }
        public void setPrimaryKeyColumns(List<String> primaryKeyColumns) {
            this.primaryKeyColumns = new ArrayList<>(primaryKeyColumns);
        }
        public String getSourcePrimaryKeySha256() { return sourcePrimaryKeySha256; }
        public void setSourcePrimaryKeySha256(String sourcePrimaryKeySha256) {
            this.sourcePrimaryKeySha256 = sourcePrimaryKeySha256;
        }
        public String getTargetPrimaryKeySha256() { return targetPrimaryKeySha256; }
        public void setTargetPrimaryKeySha256(String targetPrimaryKeySha256) {
            this.targetPrimaryKeySha256 = targetPrimaryKeySha256;
        }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
