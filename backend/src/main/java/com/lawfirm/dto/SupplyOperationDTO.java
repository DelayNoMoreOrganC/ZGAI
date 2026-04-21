package com.lawfirm.dto;

import lombok.Data;

/**
 * 办公用品操作DTO（入库/出库）
 */
@Data
public class SupplyOperationDTO {

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 经办人
     */
    private String operator;

    /**
     * 领用人（出库时）
     */
    private String receiver;

    /**
     * 用途（出库时）
     */
    private String purpose;

    /**
     * 备注
     */
    private String remark;
}
