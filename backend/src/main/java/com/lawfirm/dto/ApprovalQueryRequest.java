package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.validation.constraints.AssertTrue;
import java.time.LocalDate;

/**
 * 审批查询请求
 */
@Data
public class ApprovalQueryRequest {
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须从1开始")
    private Integer page = 1;

    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 20;

    @NotNull(message = "排序字段不能为空")
    @Pattern(regexp = "applyTime|approvedTime|createdAt|status|title", message = "不支持的排序字段")
    private String sortField = "applyTime";

    @NotNull(message = "排序方向不能为空")
    @Pattern(regexp = "(?i)ASC|DESC", message = "排序方向只能是ASC或DESC")
    private String sortDirection = "DESC";

    @Size(max = 40, message = "审批类型不能超过40个字符")
    private String approvalType;

    @Size(max = 20, message = "审批状态不能超过20个字符")
    @Pattern(regexp = "PENDING|APPROVED|REJECTED|TRANSFERRED|WITHDRAWN", message = "不支持的审批状态")
    private String status;
    @Pattern(regexp = "PROCESSED", message = "不支持的审批状态分组")
    private String statusGroup;
    private Long applicantId;
    private Long currentApproverId;
    private Long caseId;
    @Size(max = 200, message = "搜索关键词不能超过200个字符")
    private String keyword;
    private LocalDate startDate;
    private LocalDate endDate;

    @AssertTrue(message = "申请开始日期不能晚于结束日期")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !startDate.isAfter(endDate);
    }
}
