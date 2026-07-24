package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "law_firm_letter", indexes = {
        @Index(name = "idx_law_firm_letter_case", columnList = "case_id"),
        @Index(name = "idx_law_firm_letter_approval", columnList = "approval_id"),
        @Index(name = "idx_law_firm_letter_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_law_firm_letter_approval", columnNames = "approval_id")
})
public class LawFirmLetter extends LogicalDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "recipient", nullable = false, length = 300)
    private String recipient;

    @Column(name = "client_name", nullable = false, length = 500)
    private String clientName;

    @Column(name = "lawyer_names", nullable = false, length = 1000)
    private String lawyerNames;

    @Column(name = "opposing_party", nullable = false, length = 1000)
    private String opposingParty;

    @Column(name = "case_reason", nullable = false, length = 500)
    private String caseReason;

    @Column(name = "letter_type_code", nullable = false, length = 10)
    private String letterTypeCode;

    @Column(name = "lawyer_contacts", nullable = false, length = 1000)
    private String lawyerContacts;

    @Column(name = "closing_text", nullable = false, length = 100)
    private String closingText = "特此函告！";

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "serial_no")
    private Integer serialNo;

    @Column(name = "letter_number", length = 100)
    private String letterNumber;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "approval_id")
    private Long approvalId;

    @Column(name = "final_document_id")
    private Long finalDocumentId;

    @Column(name = "draft_sha256", length = 64)
    private String draftSha256;

    @Column(name = "final_sha256", length = 64)
    private String finalSha256;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_reason", length = 2000)
    private String rejectedReason;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;
}
