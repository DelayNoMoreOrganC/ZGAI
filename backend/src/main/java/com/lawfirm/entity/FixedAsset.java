package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "fixed_asset")
public class FixedAsset extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 资产名称
     */
    @NotBlank(message = "资产名称不能为空")
    @Column(name = "asset_name", nullable = false, length = 100)
    private String assetName;

    /**
     * 资产类别：办公设备/电子设备/家具/车辆/其他
     */
    @NotBlank(message = "资产类别不能为空")
    @Column(name = "asset_category", nullable = false, length = 50)
    private String assetCategory;

    /**
     * 资产编号
     */
    @Column(name = "asset_code", length = 50)
    private String assetCode;

    /**
     * 规格型号
     */
    @Column(name = "specification", length = 100)
    private String specification;

    /**
     * 购买日期
     */
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    /**
     * 购买价格
     */
    @Column(name = "purchase_price", precision = 15, scale = 2)
    private BigDecimal purchasePrice;

    /**
     * 当前价值（折旧后）
     */
    @Column(name = "current_value", precision = 15, scale = 2)
    private BigDecimal currentValue;

    /**
     * 使用部门
     */
    @Column(name = "department", length = 100)
    private String department;

    /**
     * 保管人
     */
    @Column(name = "custodian", length = 50)
    private String custodian;

    /**
     * 存放位置
     */
    @Column(name = "location", length = 100)
    private String location;

    /**
     * 使用状态：IN_USE(使用中)/IDLE(闲置)/DAMAGED(损坏)/SCRAPPED(报废)
     */
    @Column(name = "usage_status", length = 20)
    private String usageStatus = "IN_USE";

    /**
     * 折旧年限（年）
     */
    @Column(name = "depreciation_years")
    private Integer depreciationYears;

    /**
     * 折旧方式：STRAIGHT_LINE(直线法)/DOUBLE_DECLINING(双倍余额递减法)
     */
    @Column(name = "depreciation_method", length = 50)
    private String depreciationMethod = "STRAIGHT_LINE";

    /**
     * 备注
     */
    @Lob
    @Column(name = "remarks")
    private String remarks;
}
