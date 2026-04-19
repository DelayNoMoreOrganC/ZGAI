package com.lawfirm.service;

import com.lawfirm.entity.AuditLog;
import com.lawfirm.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 记录审计日志（异步）
     */
    @Async
    public void recordLog(Long userId, String module, String operation,
                         String method, String params, String ip,
                         Integer status, String errorMsg, Integer executionTime) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setModule(module);
            auditLog.setOperation(operation);
            auditLog.setMethod(method);
            auditLog.setParams(params);
            auditLog.setIp(ip);
            auditLog.setStatus(status);
            auditLog.setErrorMsg(errorMsg);
            auditLog.setExecutionTime(executionTime);
            auditLog.setCreatedAt(LocalDateTime.now());

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("保存审计日志失败", e);
        }
    }

    /**
     * 记录成功日志
     */
    @Async
    public void recordSuccess(Long userId, String module, String operation,
                             String method, String params, String ip, Integer executionTime) {
        recordLog(userId, module, operation, method, params, ip, 1, null, executionTime);
    }

    /**
     * 记录失败日志
     */
    @Async
    public void recordFailure(Long userId, String module, String operation,
                             String method, String params, String ip, String errorMsg) {
        recordLog(userId, module, operation, method, params, ip, 0, errorMsg, null);
    }

    /**
     * 获取审计日志列表
     */
    public Page<AuditLog> getAuditLogList(Long userId, String module, String operation,
                                         Integer status, LocalDateTime startTime,
                                         LocalDateTime endTime, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 这里可以添加动态查询条件
        // 为简化代码，直接返回分页结果
        return auditLogRepository.findAll(pageable);
    }

    /**
     * 获取用户的操作日志
     */
    public List<AuditLog> getUserLogs(Long userId) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取模块的统计信息
     */
    public List<Object[]> getModuleStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.getModuleStatistics(startTime, endTime);
    }

    /**
     * 获取各模块的总操作次数统计
     */
    public List<Object[]> getModuleTotalStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.getModuleTotalStatistics(startTime, endTime);
    }
}
