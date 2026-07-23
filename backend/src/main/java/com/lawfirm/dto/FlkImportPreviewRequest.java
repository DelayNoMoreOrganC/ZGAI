package com.lawfirm.dto;

import lombok.Data;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class FlkImportPreviewRequest {
    @NotEmpty(message = "请至少提交一个法规详情链接")
    @Size(max = 50, message = "单批最多提交50个链接")
    private List<String> urls;
}
