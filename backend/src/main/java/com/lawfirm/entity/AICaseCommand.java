package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_case_command", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ai_command_user_key", columnNames = {"user_id", "idempotency_key"})
}, indexes = {
        @Index(name = "idx_ai_command_case", columnList = "case_id"),
        @Index(name = "idx_ai_command_status", columnList = "status")
})
public class AICaseCommand extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "case_id")
    private Long caseId;

    @Lob
    @Column(name = "instruction", nullable = false)
    private String instruction;

    @Column(name = "instruction_hash", length = 64)
    private String instructionHash;

    @Lob
    @Column(name = "actions_json")
    private String actionsJson;

    @NotBlank
    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "clarification", length = 500)
    private String clarification;

    @Column(name = "model_name", length = 120)
    private String modelName;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "privacy_sanitized_at")
    private LocalDateTime privacySanitizedAt;
}
