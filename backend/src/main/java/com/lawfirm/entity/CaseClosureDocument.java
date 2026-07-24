package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_closure_document", uniqueConstraints = @UniqueConstraint(
        name = "uk_case_closure_document", columnNames = {"closure_request_id", "case_document_id"}))
public class CaseClosureDocument extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "closure_request_id", nullable = false)
    private Long closureRequestId;

    @Column(name = "case_document_id", nullable = false)
    private Long caseDocumentId;
}
