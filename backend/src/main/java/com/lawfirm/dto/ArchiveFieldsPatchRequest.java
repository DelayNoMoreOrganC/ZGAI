package com.lawfirm.dto;

import lombok.Data;
import javax.validation.constraints.NotEmpty;
import java.util.Map;

@Data
public class ArchiveFieldsPatchRequest {
    @NotEmpty(message = "归档字段不能为空")
    private Map<String, String> fields;
}
