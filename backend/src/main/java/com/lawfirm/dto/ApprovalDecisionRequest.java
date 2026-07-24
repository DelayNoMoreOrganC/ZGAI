package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.Size;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
public class ApprovalDecisionRequest {

    @Size(max = 2000, message = "审批意见不能超过2000个字符")
    private String comments;

    @Min(value = 1, message = "首次所函流水号不能小于1")
    @Max(value = 999999, message = "首次所函流水号不能大于999999")
    private Integer initialLetterSerial;
}
