package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 办案记录DTO
 */
@Data
public class CaseRecordDTO {

    /**
     * 记录ID（更新时使用）
     */
    private Long id;

    /**
     * 记录标题
     */
    @NotBlank(message = "记录标题不能为空")
    private String title;

    /**
     * 记录内容
     */
    @NotBlank(message = "记录内容不能为空")
    private String content;

    /**
     * 案件阶段
     */
    @NotBlank(message = "案件阶段不能为空")
    private String stage;

    /**
     * 工作时长（小时）
     */
    private BigDecimal workHours;

    /**
     * 记录日期
     */
    @NotNull(message = "记录日期不能为空")
    private LocalDate recordDate;

    /**
     * 附件URL列表
     */
    private List<String> attachmentUrls;
}
