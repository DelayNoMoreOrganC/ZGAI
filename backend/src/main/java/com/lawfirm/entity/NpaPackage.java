package com.lawfirm.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 不良资产包
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "npa_package")
public class NpaPackage extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_name", nullable = false)
    private String packageName;

    @Column(name = "package_code", unique = true, length = 50)
    private String packageCode;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "asset_count")
    private Integer assetCount;

    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;

    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(length = 20)
    private String status = "PENDING";

    @Column(name = "responsible_person", length = 50)
    private String responsiblePerson;

    @Column(name = "recovered_amount", precision = 18, scale = 2)
    private BigDecimal recoveredAmount = BigDecimal.ZERO;

    @Column(name = "cost_amount", precision = 18, scale = 2)
    private BigDecimal costAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", length = 50)
    private String createdBy;
}
