package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 旧系统资料检索请求。
 */
@Data
public class LegacyMaterialSearchRequest {

    @NotNull(message = "请选择来源案件")
    private Long caseId;

    @Min(value = 1, message = "结果上限不能小于1")
    @Max(value = 100, message = "结果上限不能大于100")
    private Integer limit;
}
