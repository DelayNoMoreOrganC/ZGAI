package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class YuandianCitationCheckRequest {

    @NotBlank(message = "待核验文本不能为空")
    @Size(max = 20000, message = "单次核验文本不能超过20000个字符")
    private String content;
}
