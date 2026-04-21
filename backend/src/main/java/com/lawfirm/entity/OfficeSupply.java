package com.lawfirm.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 办公用品实体
 */
@Entity
@Table(name = "office_supplies")
@Data
public class OfficeSupply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用品名称
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 类别（文具/耗材/设备/日用品/其他）
     */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /**
     * 规格
     */
    @Column(name = "specification", length = 100)
    private String specification;

    /**
     * 库存数量
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    /**
     * 单位（个/支/本/包/箱/件/套）
     */
    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    /**
     * 单价
     */
    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /**
     * 最低库存预警线
     */
    @Column(name = "min_stock", nullable = false)
    private Integer minStock = 10;

    /**
     * 存放位置
     */
    @Column(name = "location", length = 100)
    private String location;

    /**
     * 最近入库日期
     */
    @Column(name = "last_inbound_date")
    private LocalDate lastInboundDate;

    /**
     * 备注
     */
    @Column(name = "remark", length = 500)
    private String remark;

    /**
     * 是否删除
     */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 出入库记录（关联查询用）
     */
    @Transient
    private List<SupplyRecord> records = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
