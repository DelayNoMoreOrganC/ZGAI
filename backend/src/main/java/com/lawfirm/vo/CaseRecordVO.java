package com.lawfirm.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 办案记录视图对象
 */
@Data
public class CaseRecordVO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 案件ID
     */
    private Long caseId;

    /**
     * 记录标题
     */
    private String title;

    /**
     * 记录内容
     */
    private String content;

    /**
     * 案件阶段
     */
    private String stage;

    /**
     * 工作时长（小时）- 前端使用hours字段
     */
    private BigDecimal hours;

    /**
     * 记录日期
     */
    private LocalDate recordDate;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建人姓名
     */
    private String authorName;

    /**
     * 附件URL列表
     */
    private List<String> attachments;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否完成（前端统计使用）
     */
    private Boolean completed;
}
