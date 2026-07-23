package com.lawfirm.dto;

import lombok.Data;

/**
 * 行政人员提交的正式利冲审查结论。
 */
@Data
public class ConflictCheckReviewRequest {

    private String decision;
    private String conclusion;
    private String waiverBasis;
}
