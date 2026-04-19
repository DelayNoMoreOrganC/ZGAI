package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 当事人DTO
 */
@Data
public class PartyDTO {

    /**
     * 当事人ID（更新时使用）
     */
    private Long id;

    /**
     * 当事人类型（INDIVIDUAL/ORGANIZATION）
     */
    @NotBlank(message = "当事人类型不能为空")
    private String partyType;

    /**
     * 当事人属性（PLAINTIFF/DEFENDANT/THIRD_PARTY等）
     */
    @NotBlank(message = "当事人属性不能为空")
    private String partyRole;

    /**
     * 姓名/单位名称
     */
    @NotBlank(message = "姓名/单位名称不能为空")
    private String name;

    /**
     * 是否委托方
     */
    private Boolean isClient = false;

    /**
     * 性别（个人类型）
     */
    private String gender;

    /**
     * 民族（个人类型）
     */
    private String ethnicity;

    /**
     * 身份证号（个人类型）
     */
    private String idCard;

    /**
     * 统一社会信用代码（单位类型）
     */
    private String creditCode;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 住址/地址
     */
    private String address;

    /**
     * 法定代表人（单位类型）
     */
    private String legalRepresentative;

    /**
     * 对方律师
     */
    private String opposingLawyer;

    /**
     * 备注
     */
    private String notes;

    /**
     * 同步创建客户
     */
    private Boolean syncToClient = false;
}
