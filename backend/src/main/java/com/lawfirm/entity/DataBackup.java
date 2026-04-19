package com.lawfirm.entity;

import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 数据备份记录实体
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "data_backup")
public class DataBackup extends LogicalDeleteEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 备份文件路径
     */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /**
     * 备份类型：AUTO=自动备份，MANUAL=手动备份
     */
    @Column(name = "backup_type", nullable = false, length = 20)
    private String backupType;

    /**
     * 备份状态：SUCCESS=成功，FAILED=失败
     */
    @Column(name = "backup_status", nullable = false, length = 20)
    private String backupStatus;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 备份时间
     */
    @Column(name = "backup_time", nullable = false)
    private LocalDateTime backupTime;

    /**
     * 创建人ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 保留天数（默认180天）
     */
    @Column(name = "retention_days")
    private Integer retentionDays;

    /**
     * 备份备注
     */
    @Column(name = "remark", length = 500)
    private String remark;

    /**
     * 错误信息（备份失败时记录）
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    // Getters and Setters
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getBackupType() {
        return backupType;
    }

    public void setBackupType(String backupType) {
        this.backupType = backupType;
    }

    public String getBackupStatus() {
        return backupStatus;
    }

    public void setBackupStatus(String backupStatus) {
        this.backupStatus = backupStatus;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getBackupTime() {
        return backupTime;
    }

    public void setBackupTime(LocalDateTime backupTime) {
        this.backupTime = backupTime;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
