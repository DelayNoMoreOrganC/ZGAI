package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "dictionary")
public class Dictionary extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "字典类型不能为空")
    @Column(name = "dict_type", nullable = false, length = 50)
    private String dictType;

    @NotBlank(message = "字典标签不能为空")
    @Column(name = "dict_label", nullable = false, length = 100)
    private String dictLabel;

    @NotBlank(message = "字典值不能为空")
    @Column(name = "dict_value", nullable = false, length = 100)
    private String dictValue;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column
    private Integer status = 1;

    @Column
    private String remark;
}
