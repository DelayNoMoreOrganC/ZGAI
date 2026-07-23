package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 客户与关联企业、实际控制人、曾用名等主体之间的显式关系。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "client_subject_relation", indexes = {
        @Index(name = "idx_client_relation_source", columnList = "source_client_id"),
        @Index(name = "idx_client_relation_target", columnList = "target_client_id"),
        @Index(name = "idx_client_relation_target_name", columnList = "target_subject_name"),
        @Index(name = "idx_client_relation_deleted", columnList = "deleted")
})
public class ClientSubjectRelation extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_client_id", nullable = false)
    private Long sourceClientId;

    @Column(name = "target_client_id")
    private Long targetClientId;

    @Column(name = "target_subject_name", nullable = false, length = 200)
    private String targetSubjectName;

    @Column(name = "target_credit_code", length = 50)
    private String targetCreditCode;

    @Column(name = "relation_type", nullable = false, length = 40)
    private String relationType;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;
}
