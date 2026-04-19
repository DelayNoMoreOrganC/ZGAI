package com.lawfirm.entity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 支持逻辑删除的基础实体类
 */
@Data
@MappedSuperclass
public abstract class LogicalDeleteEntity extends BaseEntity {

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
}
