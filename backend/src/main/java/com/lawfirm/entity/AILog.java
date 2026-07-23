package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * AI使用日志实体
 */
@Entity
@Table(name = "ai_log")
public class AILog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "case_id")
    private Long caseId;

    @NotBlank(message = "AI功能类型不能为空")
    @Column(name = "function_type", nullable = false, length = 50)
    private String functionType;

    @Column(name = "input_content")
    @Lob
    private String inputContent;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_content")
    @Lob
    private String outputContent;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "model_name", length = 50)
    private String modelName;

    @Column(name = "provider_type", length = 50)
    private String providerType;

    @Column(name = "input_summary", length = 500)
    private String inputSummary;

    @Column(name = "input_hash", length = 64)
    private String inputHash;

    @Column(name = "output_hash", length = 64)
    private String outputHash;

    @Column(name = "estimated_cost_micros")
    private Long estimatedCostMicros;

    @Column(name = "privacy_sanitized_at")
    private LocalDateTime privacySanitizedAt;

    @Column(length = 20)
    private String status;

    @Column
    private Integer duration;

    @Column(name = "error_message")
    @Lob
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getFunctionType() {
        return functionType;
    }

    public void setFunctionType(String functionType) {
        this.functionType = functionType;
    }

    public String getInputContent() {
        return inputContent;
    }

    public void setInputContent(String inputContent) {
        this.inputContent = inputContent;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }

    public String getOutputContent() {
        return outputContent;
    }

    public void setOutputContent(String outputContent) {
        this.outputContent = outputContent;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getProviderType() { return providerType; }

    public void setProviderType(String providerType) { this.providerType = providerType; }

    public String getInputSummary() { return inputSummary; }

    public void setInputSummary(String inputSummary) { this.inputSummary = inputSummary; }

    public String getInputHash() { return inputHash; }

    public void setInputHash(String inputHash) { this.inputHash = inputHash; }

    public String getOutputHash() { return outputHash; }

    public void setOutputHash(String outputHash) { this.outputHash = outputHash; }

    public Long getEstimatedCostMicros() { return estimatedCostMicros; }

    public void setEstimatedCostMicros(Long estimatedCostMicros) { this.estimatedCostMicros = estimatedCostMicros; }

    public LocalDateTime getPrivacySanitizedAt() { return privacySanitizedAt; }

    public void setPrivacySanitizedAt(LocalDateTime privacySanitizedAt) { this.privacySanitizedAt = privacySanitizedAt; }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
