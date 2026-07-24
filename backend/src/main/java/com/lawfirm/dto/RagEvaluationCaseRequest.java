package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Data
public class RagEvaluationCaseRequest {
    @NotBlank(message = "样本名称不能为空")
    @Size(max = 120, message = "样本名称不能超过120个字符")
    private String name;

    @NotBlank(message = "评价问题不能为空")
    @Size(max = 1000, message = "评价问题不能超过1000个字符")
    private String question;

    @NotEmpty(message = "至少选择一篇预期命中文档")
    private List<Long> expectedArticleIds = new ArrayList<>();

    private List<Long> forbiddenArticleIds = new ArrayList<>();

    private Boolean enabled = true;
}
