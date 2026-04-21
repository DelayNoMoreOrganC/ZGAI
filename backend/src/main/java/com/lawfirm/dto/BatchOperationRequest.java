package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量操作请求DTO
 */
@Data
public class BatchOperationRequest {

    /**
     * 案件ID列表
     */
    @NotEmpty(message = "案件ID列表不能为空")
    private List<Long> caseIds;

    /**
     * 批量操作类型
     */
    private String operation; // close, archive, delete, changeOwner

    /**
     * 操作参数（用于修改主办律师等）
     */
    private Long ownerId;

    /**
     * 操作原因（用于状态变更等）
     */
    private String reason;
}
