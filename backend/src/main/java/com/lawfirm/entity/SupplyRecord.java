package com.lawfirm.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 办公用品出入库记录
 */
@Entity
@Table(name = "supply_records")
@Data
public class SupplyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用品ID
     */
    @Column(name = "supply_id", nullable = false)
    private Long supplyId;

    /**
     * 记录类型（入库/出库）
     */
    @Column(name = "type", nullable = false, length = 20)
    private String type;

    /**
     * 数量
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 经办人
     */
    @Column(name = "operator", length = 50)
    private String operator;

    /**
     * 领用人（出库时）
     */
    @Column(name = "receiver", length = 50)
    private String receiver;

    /**
     * 用途（出库时）
     */
    @Column(name = "purpose", length = 200)
    private String purpose;

    /**
     * 备注
     */
    @Column(name = "remark", length = 500)
    private String remark;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
