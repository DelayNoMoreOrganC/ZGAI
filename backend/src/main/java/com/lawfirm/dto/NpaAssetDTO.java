package com.lawfirm.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 债权 DTO
 */
@Data
public class NpaAssetDTO {
    private Long id;
    private Long packageId;
    private String packageName;
    private String debtorName;
    private String debtorIdNumber;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal totalAmount;
    private String guaranteeType;
    private String collateral;
    private String guaranteePerson;
    private String lawsuitStatus;
    private String court;
    private String caseNumber;
    private String judge;
    private Long relatedCaseId;
    private String status;
    private BigDecimal estimatedRecovery;
    private BigDecimal recoveredAmount;
    private BigDecimal recoveryRate;
    private String riskLevel;
    private String address;
    private String remark;
}
