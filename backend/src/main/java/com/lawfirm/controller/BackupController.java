package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.entity.DataBackup;
import com.lawfirm.entity.User;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.service.BackupService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;

    /**
     * 手动触发备份
     */
    @PostMapping("/backup")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @AuditLog(value = "手动数据备份", operationType = "BACKUP", logParams = false)
    public Result<Map<String, Object>> manualBackup(@RequestParam(required = false) String remark) {
        Long userId = getCurrentUserId();

        DataBackup backup = backupService.manualBackup(remark, userId);

        Map<String, Object> data = new HashMap<>();
        data.put("backupId", backup.getId());
        data.put("filePath", backup.getFilePath());
        data.put("fileSize", backup.getFileSize());
        data.put("backupTime", backup.getBackupTime());
        data.put("status", backup.getBackupStatus());

        return Result.success(data);
    }

    /**
     * 获取备份列表
     */
    @GetMapping("/backups")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Result<List<DataBackup>> getBackupList() {
        List<DataBackup> backups = backupService.getBackupList();
        return Result.success(backups);
    }

    /**
     * 恢复数据（危险操作）
     */
    @PostMapping("/restore/{backupId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @AuditLog(value = "恢复数据", operationType = "RESTORE", logParams = false)
    public Result<Void> restoreData(@PathVariable Long backupId) {
        Long userId = getCurrentUserId();
        log.warn("用户 {} 请求从备份 {} 恢复数据", userId, backupId);

        backupService.restoreData(backupId, userId);

        return Result.success();
    }

    /**
     * 删除备份
     */
    @DeleteMapping("/backup/{backupId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
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
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByUsername(username)
                    .map(User::getId)
                    .orElse(0L); // 0表示系统用户
        } catch (Exception e) {
            return 0L; // 异常情况返回系统用户ID
        }
    }
}
