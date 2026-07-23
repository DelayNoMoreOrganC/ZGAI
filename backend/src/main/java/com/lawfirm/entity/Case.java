package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 案件实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "\"case\"", indexes = {
    @Index(name = "idx_case_name", columnList = "case_name"),
    @Index(name = "idx_case_status", columnList = "status"),
    @Index(name = "idx_case_owner", columnList = "owner_id"),
    @Index(name = "idx_case_created", columnList = "created_at"),
    @Index(name = "idx_case_deleted", columnList = "deleted")
})
public class Case extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_number", unique = true, length = 50)
    private String caseNumber;

    @NotBlank(message = "案件名称不能为空")
    @Column(name = "case_name", nullable = false)
    private String caseName;

    @NotBlank(message = "案件类型不能为空")
    @Column(name = "case_type", nullable = false, length = 20)
    private String caseType;

    @Column(name = "case_reason", length = 100)
    private String caseReason;

    @Column(length = 20)
    private String procedure;

    @Column(length = 20)
    private String level = "GENERAL";

    @Column(length = 20)
    private String status = "CONSULTATION";

    @Column(name = "current_stage", length = 50)
    private String currentStage;

    @Column(length = 100)
    private String court;

    @Column(name = "acceptance_date")
    private LocalDate acceptanceDate;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(name = "commission_date")
    private LocalDate commissionDate;

    @Column(name = "suspect_name", length = 100)
    private String suspectName;

    @Column(name = "subject_matter")
    private String subjectMatter;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(name = "agency_type", length = 50)
    private String agencyType;

    @Column(name = "service_start_date")
    private LocalDate serviceStartDate;

    @Column(name = "service_end_date")
    private LocalDate serviceEndDate;

    @Column(name = "consultant_unit_name", length = 200)
    private String consultantUnitName;

    @Column(name = "consultant_contact_name", length = 100)
    private String consultantContactName;

    @Column(name = "consultant_contact_department", length = 100)
    private String consultantContactDepartment;

    @Column(name = "consultant_contact_title", length = 100)
    private String consultantContactTitle;

    @Column(name = "consultant_contact_phone", length = 30)
    private String consultantContactPhone;

    @Column(name = "consultant_contact_email", length = 120)
    private String consultantContactEmail;

    @Column(name = "consultant_service_scope", length = 2000)
    private String consultantServiceScope;

    @Column(name = "consultant_response_requirement", length = 500)
    private String consultantResponseRequirement;

    @Column(name = "consultant_included_services", length = 2000)
    private String consultantIncludedServices;

    @Column(name = "consultant_excluded_services", length = 2000)
    private String consultantExcludedServices;

    @Column(name = "renewal_reminder_date")
    private LocalDate renewalReminderDate;

    @Column(name = "trial_stages")
    private String trialStages;

    @Column(name = "court_case_number", length = 100)
    private String courtCaseNumber;

    @Column(name = "hearing_date")
    private LocalDate hearingDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "close_status", length = 20)
    private String closeStatus;

    @Column(name = "archive_date")
    private LocalDate archiveDate;

    @Column(name = "archive_location")
    private String archiveLocation;

    @Column(name = "case_folder_path")
    private String caseFolderPath;

    private String summary;

    @Column(length = 500)
    private String tags;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "attorney_fee", precision = 15, scale = 2)
    private BigDecimal attorneyFee;

    @Column(name = "fee_method", length = 50)
    private String feeMethod;

    @Column(name = "risk_ratio", precision = 5, scale = 2)
    private BigDecimal riskRatio;

    @Column(name = "risk_fee", precision = 15, scale = 2)
    private BigDecimal riskFee;

    @Column(name = "fee_notes")
    private String feeNotes;

    @Column(name = "free_reason")
    private String freeReason;

    @Column(name = "allocation_json", length = 2000)
    private String allocationJson;

    @Column(name = "won_amount", precision = 15, scale = 2)
    private BigDecimal wonAmount;

    @Column(name = "actual_received", precision = 15, scale = 2)
    private BigDecimal actualReceived;

    @NotNull(message = "主办律师不能为空")
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * 客户ID（关联客户）
     */
    @Column(name = "client_id")
    private Long clientId;
}
