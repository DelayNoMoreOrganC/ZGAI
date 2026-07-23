package com.lawfirm.dto;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class ArchiveDocumentPatchRequest {
    @Valid
    @NotEmpty(message = "至少提交一项文档调整")
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "归档文档项ID不能为空")
        private Long id;
        @NotNull(message = "是否归档不能为空")
        private Boolean included;
        private Integer catalogSeq;
        @Size(max = 200, message = "目录名称不能超过200个字符")
        private String catalogName;
    }
}
