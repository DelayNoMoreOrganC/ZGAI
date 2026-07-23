package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class YuandianSearchRequest {

    @NotBlank(message = "检索内容不能为空")
    @Size(max = 1000, message = "检索内容不能超过1000个字符")
    private String query;

    @Min(value = 1, message = "返回数量不能小于1")
    @Max(value = 20, message = "返回数量不能超过20")
    private Integer limit = 8;

    private Boolean onlyEffective = true;

    private Boolean onlyAuthoritative = true;
}
