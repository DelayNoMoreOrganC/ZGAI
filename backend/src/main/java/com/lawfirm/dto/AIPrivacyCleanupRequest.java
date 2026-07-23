package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class AIPrivacyCleanupRequest {

    @NotNull(message = "请选择清理前备份")
    private Long backupId;

    @NotBlank(message = "请输入清理确认词")
    @Pattern(regexp = "清理历史AI敏感原文", message = "清理确认词不正确")
    private String confirmation;
}
