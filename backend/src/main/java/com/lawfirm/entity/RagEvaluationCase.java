package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "rag_evaluation_case")
public class RagEvaluationCase extends LogicalDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(name = "expected_article_ids", nullable = false, length = 2000)
    private String expectedArticleIds;

    @Column(name = "forbidden_article_ids", length = 2000)
    private String forbiddenArticleIds;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

}
