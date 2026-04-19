package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "system_config")
public class SystemConfig extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "配置键不能为空")
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    private String configValue;

    @Column(name = "config_type", length = 20)
    private String configType = "STRING";

    @Column(length = 50)
    private String category;

    @Column
    private String description;
}
