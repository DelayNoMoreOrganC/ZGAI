package com.lawfirm.dto;

import lombok.Data;
import javax.validation.constraints.Size;

@Data
public class ArchiveJobCreateRequest {
    @Size(max = 100, message = "幂等键不能超过100个字符")
    private String idempotencyKey;

    @Size(max = 1000, message = "归档更正原因不能超过1000个字符")
    private String correctionReason;
}
