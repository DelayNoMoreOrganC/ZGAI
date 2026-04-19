package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 当事人实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "party")
public class Party extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "案件ID不能为空")
    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @NotBlank(message = "当事人类型不能为空")
    @Column(name = "party_type", nullable = false, length = 20)
    private String partyType;

    @NotBlank(message = "当事人属性不能为空")
    @Column(name = "party_role", nullable = false, length = 20)
    private String partyRole;

    @NotBlank(message = "姓名/单位名称不能为空")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_client")
    private Boolean isClient = false;

    @Column(length = 10)
    private String gender;

    @Column(length = 20)
    private String ethnicity;

    @Column(name = "id_card", length = 20)
    private String idCard;

    @Column(name = "credit_code", length = 50)
    private String creditCode;

    @Column(length = 20)
    private String phone;

    @Column
    private String address;

    @Column(name = "legal_representative", length = 50)
    private String legalRepresentative;

    @Column(name = "opposing_lawyer", length = 50)
    private String opposingLawyer;

    private String notes;
}
