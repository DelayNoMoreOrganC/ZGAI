package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 公告实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "announcement")
public class Announcement extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "公告标题不能为空")
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "公告内容不能为空")
    @Column(nullable = false)
    private String content;

    @NotNull(message = "发布时间不能为空")
    @Column(name = "publish_date", nullable = false)
    private LocalDateTime publishDate;

    @NotNull(message = "发布人不能为空")
    @Column(name = "publisher_id", nullable = false)
    private Long publisherId;

    @Column(name = "target_scope", length = 20)
    private String targetScope = "ALL";

    @Column
    private Integer priority = 0;

    @Column
    private String attachments;
}
