package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 办公用品实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "office_supplies")
public class OfficeSupplies extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 物品名称
     */
    @NotBlank(message = "物品名称不能为空")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 物品类别：文具/纸品/电子配件/日常用品/其他
     */
    @NotBlank(message = "物品类别不能为空")
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /**
     * 规格型号
     */
    @Column(name = "specification", length = 100)
    private String specification;

    /**
     * 单位：个/本/盒/包等
     */
    @Column(name = "unit", length = 20)
    private String unit;

    /**
     * 当前库存数量
     */
    @NotNull(message = "库存数量不能为空")
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    /**
     * 最低库存预警
     */
    @Column(name = "min_stock")
    private Integer minStock = 5;

    /**
     * 单价
     */
    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * 供应商
     */
    @Column(name = "supplier", length = 100)
    private String supplier;

    /**
     * 存放位置
     */
    @Column(name = "location", length = 100)
    private String location;

    /**
     * 状态：IN_STOCK(在库)/LOW_STOCK(库存不足)/OUT_OF_STOCK(缺货)
     */
    @Column(name = "status", length = 20)
    private String status = "IN_STOCK";

    /**
     * 最后入库时间
     */
    @Column(name = "last_stock_in_date")
    private LocalDate lastStockInDate;

    /**
     * 备注
     */
    @Lob
    @Column(name = "remarks")
    private String remarks;
}
