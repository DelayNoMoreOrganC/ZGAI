package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 利益冲突检查记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "conflict_check_record", indexes = {
    @Index(name = "idx_conflict_check_subject", columnList = "subject_name"),
    @Index(name = "idx_conflict_check_operator", columnList = "checked_by"),
    @Index(name = "idx_conflict_check_level", columnList = "conflict_level"),
    @Index(name = "idx_conflict_check_case", columnList = "case_id")
})
public class ConflictCheckRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "检查对象不能为空")
    @Column(name = "subject_name", nullable = false, length = 100)
    private String subjectName;

    @Column(name = "client_type", length = 30)
    private String clientType;

    @Column(name = "client_relationship", length = 30)
    private String clientRelationship;

    @Column(name = "client_role", length = 30)
    private String clientRole;

    @Column(name = "id_card", length = 20)
    private String idCard;

    @Column(name = "credit_code", length = 50)
    private String creditCode;

    @Column(name = "checked_by")
    private Long checkedBy;

    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "matched_client_ids", length = 1000)
    private String matchedClientIds;

    @Column(name = "matched_case_ids", length = 1000)
    private String matchedCaseIds;

    @Column(name = "similar_names", length = 1000)
    private String similarNames;

    @Column(name = "matched_related_subjects", length = 2000)
    private String matchedRelatedSubjects;

    @Column(name = "conflict_level", length = 30)
    private String conflictLevel;

    @Column(name = "conclusion", length = 500)
    private String conclusion;

    @Column(name = "remark", length = 1000)
    private String remark;

    @Column(name = "review_status", length = 30)
    private String reviewStatus = "PENDING_REVIEW";

    @Column(name = "review_decision", length = 30)
    private String reviewDecision;

    @Column(name = "review_conclusion", length = 1000)
    private String reviewConclusion;

    @Column(name = "waiver_basis", length = 2000)
    private String waiverBasis;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "archived_document_id")
    private Long archivedDocumentId;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
}
