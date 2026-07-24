package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class CaseClosureCreateRequest {
    @NotBlank(message = "请选择结案方式")
    private String closureType;

    @NotBlank(message = "请填写案件结果")
    @Size(min = 2, max = 1000, message = "案件结果长度应为2-1000个字符")
    private String caseOutcome;

    @NotBlank(message = "请填写结案小结")
    @Size(min = 20, max = 5000, message = "结案小结长度应为20-5000个字符")
    private String closureSummary;

    @NotBlank(message = "请选择费用处理状态")
    private String feeStatus;

    @NotBlank(message = "请选择客户交付状态")
    private String clientDeliveryStatus;

    @Size(max = 1000, message = "客户交付说明不能超过1000个字符")
    private String clientDeliveryNotes;

    @AssertTrue(message = "请确认案件材料已经核对")
    @NotNull(message = "请确认案件材料已经核对")
    private Boolean documentsConfirmed;

    @NotEmpty(message = "请至少选择一份结案依据文件")
    private List<Long> basisDocumentIds;
}
