package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.entity.DataBackup;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.BackupService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据备份控制器
 */
@Slf4j
@RestController
@RequestMapping("system")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;
    private final SecurityUtils securityUtils;

    /**
     * 手动触发备份
     */
    @PostMapping("/backup")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    @AuditLog(value = "手动数据备份", operationType = "BACKUP", logParams = false)
    public Result<Map<String, Object>> manualBackup(@RequestParam(required = false) String remark) {
        Long userId = getCurrentUserId();

        DataBackup backup = backupService.manualBackup(remark, userId);

        Map<String, Object> data = new HashMap<>();
        data.put("backupId", backup.getId());
        data.put("fileName", java.nio.file.Paths.get(backup.getFilePath()).getFileName().toString());
        data.put("fileSize", backup.getFileSize());
        data.put("backupTime", backup.getBackupTime());
        data.put("status", backup.getBackupStatus());

        return Result.success(data);
    }

    /**
     * 获取备份列表
     */
    @GetMapping("/backups")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public Result<List<DataBackup>> getBackupList() {
        List<DataBackup> backups = backupService.getBackupList();
        return Result.success(backups);
    }

    /**
     * 恢复数据（危险操作）
     */
    @PostMapping("/restore/{backupId}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    @AuditLog(value = "恢复数据", operationType = "RESTORE", logParams = false)
    public Result<Void> restoreData(@PathVariable Long backupId) {
        Long userId = getCurrentUserId();
        log.warn("用户 {} 请求从备份 {} 恢复数据", userId, backupId);

        boolean restored = backupService.restoreData(backupId, userId);
        return restored ? Result.success() : Result.error("恢复数据失败");
    }

    /**
     * 删除备份
     */
    @DeleteMapping("/backup/{backupId}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    @AuditLog(value = "删除备份", operationType = "DELETE", logParams = false)
    public Result<Void> deleteBackup(@PathVariable Long backupId) {
        boolean success = backupService.deleteBackup(backupId);

        if (success) {
            return Result.success();
        } else {
            return Result.error("删除备份失败");
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        return securityUtils.getCurrentUserId();
    }
}
