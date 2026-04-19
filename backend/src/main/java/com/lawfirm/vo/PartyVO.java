package com.lawfirm.vo;

import lombok.Data;

/**
 * 当事人VO
 */
@Data
public class PartyVO {

    /**
     * 当事人ID
     */
    private Long id;

    /**
     * 案件ID
     */
    private Long caseId;

    /**
     * 当事人类型
     */
    private String partyType;

    /**
     * 当事人类型描述
     */
    private String partyTypeDesc;

    /**
     * 当事人属性
     */
    private String partyRole;

    /**
     * 当事人属性描述
     */
    private String partyRoleDesc;

    /**
     * 姓名/单位名称
     */
    private String name;

    /**
     * 是否委托方
     */
    private Boolean isClient;

    /**
     * 性别
     */
    private String gender;

    /**
     * 民族
     */
    private String ethnicity;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 统一社会信用代码
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
     * 法定代表人
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
     * 创建时间
     */
    private String createdAt;
}
