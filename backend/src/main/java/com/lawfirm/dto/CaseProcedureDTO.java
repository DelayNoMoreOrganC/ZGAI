package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 案件程序DTO
 */
@Data
public class CaseProcedureDTO {

    /**
     * 程序ID（更新时使用）
     */
    private Long id;

    /**
     * 程序名称
     */
    @NotBlank(message = "程序名称不能为空")
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
}
