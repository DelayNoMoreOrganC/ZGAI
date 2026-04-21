package com.lawfirm.dto;

import com.lawfirm.entity.SupplyRecord;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 办公用品DTO
 */
@Data
public class OfficeSupplyDTO {

    private Long id;
    private String name;
    private String category;
    private String specification;
    private Integer quantity;
    private String unit;
    private BigDecimal unitPrice;
    private Integer minStock;
    private String location;
    private LocalDate lastInboundDate;
    private String remark;
    private List<SupplyRecord> records;
}
