package com.lawfirm.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ArchiveReviewRequest {
    @NotBlank(message = "复核决定不能为空")
    private String decision;

    @Size(max = 1000, message = "复核理由不能超过1000个字符")
    private String reason;

    @Size(max = 1000, message = "缺件例外理由不能超过1000个字符")
    private String exceptionReason;
}
