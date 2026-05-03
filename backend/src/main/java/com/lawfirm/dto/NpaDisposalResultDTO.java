package com.lawfirm.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class NpaDisposalResultDTO {
    private Long id;
    private Long assetId;
    private Long planId;
    private String debtorName;
    private LocalDate disposalDate;
    private BigDecimal actualRecovery;
    private BigDecimal recoveryRate;
    private BigDecimal costAmount;
    private BigDecimal netRecovery;
    private String recoveryMethod;
    private String recoveryAccount;
    private String resultDescription;
    private String status;
    private String attachments;
}
