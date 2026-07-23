package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class YuandianImportRequest {

    @NotBlank(message = "导入凭证不能为空")
    private String importToken;
}
