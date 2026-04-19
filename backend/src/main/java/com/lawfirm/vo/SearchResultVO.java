package com.lawfirm.vo;

import lombok.Data;

/**
 * 全局搜索结果VO
 */
@Data
public class SearchResultVO {

    /**
     * 结果类型：CASE/CLIENT/DOCUMENT
     */
    private String type;

    /**
     * 结果类型描述
     */
    private String typeDesc;

    /**
     * 标题
     */
    private String title;

    /**
     * 副标题
     */
    private String subtitle;

    /**
     * 匹配的字段
     */
    private String matchField;

    /**
     * 跳转链接
     */
    private String url;

    /**
     * 案件ID（如果是案件类型）
     */
    private Long caseId;

    /**
     * 客户ID（如果是客户类型）
     */
    private Long clientId;

    /**
     * 文档ID（如果是文档类型）
     */
    private Long documentId;
}
