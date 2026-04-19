package com.lawfirm.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 案件程序VO
 */
@Data
public class CaseProcedureVO {

    /**
     * 程序ID
     */
    private Long id;

    /**
     * 案件ID
     */
    private Long caseId;

    /**
     * 程序名称
     */
    private String procedureName;

    /**
     * 程序案号
     */
    private String procedureNumber;

    /**
     * 程序类型
     */
    private String procedureType;

    /**
     * 程序类型描述
     */
    private String procedureTypeDesc;

    /**
     * 立案日期
     */
    private LocalDate filingDate;

    /**
     * 开庭日期
     */
    private LocalDate hearingDate;

    /**
     * 裁决日期
     */
    private LocalDate judgmentDate;

    /**
     * 裁决结果
     */
    private String result;

    /**
     * 法院
     */
    private String court;

    /**
     * 地区
     */
    private String region;

    /**
     * 承办人/法官
     */
    private String judge;

    /**
     * 备注
     */
    private String notes;

    /**
     * 附件
     */
    private String attachments;

    /**
     * 创建时间
     */
    private String createdAt;
}
