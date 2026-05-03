package com.lawfirm.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class NpaDisposalPlanDTO {
    private Long id;
    private Long assetId;
    private String debtorName;
    private String planName;
    private String disposalMethod;
    private BigDecimal targetAmount;
    private LocalDate deadline;
    private String responsiblePerson;
    private String planDetail;
    private String status;
    private String approvalComment;
    private LocalDate startDate;
    private LocalDate completionDate;
}
