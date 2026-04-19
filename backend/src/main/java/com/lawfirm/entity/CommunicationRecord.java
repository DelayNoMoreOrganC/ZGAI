package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 沟通记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "communication_record")
public class CommunicationRecord extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "客户ID不能为空")
    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @NotBlank(message = "沟通方式不能为空")
    @Column(name = "communication_type", nullable = false, length = 20)
    private String communicationType;

    @NotNull(message = "沟通时间不能为空")
    @Column(name = "communication_date", nullable = false)
    private LocalDate communicationDate;

    @NotBlank(message = "沟通内容不能为空")
    @Column(nullable = false)
    private String content;

    @NotNull(message = "记录人不能为空")
    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "next_follow_date")
    private LocalDate nextFollowDate;

    @Column
    private String attachments;
}
