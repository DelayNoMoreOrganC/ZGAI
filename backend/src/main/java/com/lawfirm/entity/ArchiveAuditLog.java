package com.lawfirm.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "archive_audit_log", indexes = @Index(name = "idx_archive_audit_job", columnList = "job_id"))
public class ArchiveAuditLog extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "detail", length = 2000)
    private String detail;
}
