package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "client", indexes = {
    @Index(name = "idx_client_name", columnList = "client_name"),
    @Index(name = "idx_client_phone", columnList = "phone"),
    @Index(name = "idx_client_status", columnList = "status"),
    @Index(name = "idx_client_owner", columnList = "owner_id"),
    @Index(name = "idx_client_created", columnList = "created_at"),
    @Index(name = "idx_client_deleted", columnList = "deleted")
})
public class Client extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "客户类型不能为空")
    @Column(name = "client_type", nullable = false, length = 20)
    private String clientType;

    @NotBlank(message = "客户姓名/名称不能为空")
    @Column(name = "client_name", nullable = false, length = 100)
    private String clientName;

    @Column(name = "client_relationship", length = 30)
    private String clientRelationship;

    @Column(name = "client_role", length = 30)
    private String clientRole;

    @Column(length = 10)
    private String gender;

    @Column(length = 30)
    private String ethnicity;

    @Column(name = "id_card", length = 20)
    private String idCard;

    @Column(name = "credit_code", length = 50)
    private String creditCode;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column
    private String address;

    @Column(name = "contact_person", length = 50)
    private String contactPerson;

    @Column(length = 50)
    private String wechat;

    @Column(name = "legal_representative", length = 50)
    private String legalRepresentative;

    @Column(name = "legal_representative_id_card", length = 20)
    private String legalRepresentativeIdCard;

    @Column(name = "invoice_title", length = 100)
    private String invoiceTitle;

    @Column(name = "invoice_tax_no", length = 50)
    private String invoiceTaxNo;

    @Column(name = "invoice_address_phone", length = 200)
    private String invoiceAddressPhone;

    @Column(name = "invoice_bank_account", length = 200)
    private String invoiceBankAccount;

    @Column(name = "opposing_lawyer", length = 100)
    private String opposingLawyer;

    @Column(length = 50)
    private String industry;

    @Column(length = 20)
    private String status = "ACTIVE";

    @Column
    private String source;

    private String notes;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "source_user_ids", length = 500)
    private String sourceUserIds;

    @Column(name = "client_owner_ids", length = 500)
    private String clientOwnerIds;
}
