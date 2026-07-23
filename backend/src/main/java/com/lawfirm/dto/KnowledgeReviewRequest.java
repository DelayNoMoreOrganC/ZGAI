package com.lawfirm.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class KnowledgeReviewRequest {
    @NotBlank(message = "审核结论不能为空")
    private String decision;
    private String reason;
}
