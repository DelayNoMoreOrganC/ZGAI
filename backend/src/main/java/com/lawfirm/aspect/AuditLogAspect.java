package com.lawfirm.aspect;

import com.lawfirm.entity.AuditLog;
import com.lawfirm.entity.User;
import com.lawfirm.repository.AuditLogRepository;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作审计日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Around("@annotation(com.lawfirm.annotation.AuditLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取注解
        com.lawfirm.annotation.AuditLog auditAnnotation = method.getAnnotation(com.lawfirm.annotation.AuditLog.class);
        if (auditAnnotation == null) {
            return joinPoint.proceed();
        }

        // 获取请求信息
        HttpServletRequest request = getRequest();
        String ip = getClientIp(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : "";

        // 获取当前用户
        Long userId = getCurrentUserId();
        String username = getCurrentUsername();

        // 获取操作信息
        String operation = auditAnnotation.value().isEmpty() ?
                method.getName() : auditAnnotation.value();
        String operationType = auditAnnotation.operationType();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();

        // 记录请求参数
        String params = "";
        if (auditAnnotation.logParams()) {
            params = Arrays.toString(joinPoint.getArgs());
            if (params.length() > 1000) {
                params = params.substring(0, 1000) + "...";
            }
        }

        Object result = null;
        String resultStr = "";
        boolean success = true;
        String errorMsg = "";

        long startTime = System.currentTimeMillis();

        try {
            // 执行方法
            result = joinPoint.proceed();

            // 记录结果
            if (auditAnnotation.logResult() && result != null) {
                resultStr = result.toString();
                if (resultStr.length() > 1000) {
                    resultStr = resultStr.substring(0, 1000) + "...";
                }
            }

            return result;
        } catch (Exception e) {
            success = false;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            // 计算执行时间
            long duration = System.currentTimeMillis() - startTime;

            // 异步保存审计日志
            saveAuditLog(userId, operation, className, methodName,
                    params, success, errorMsg, ip, duration);
        }
    }

    /**
     * 保存审计日志
     */
    private void saveAuditLog(Long userId, String operation, String className, String methodName,
                              String params, boolean success, String errorMsg,
                              String ip, long duration) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setModule(className); // 使用className作为module
            auditLog.setOperation(operation);
            auditLog.setMethod(methodName);
            auditLog.setParams(params);
            auditLog.setIp(ip);
            auditLog.setStatus(success ? 1 : 0); // 1=成功, 0=失败
            auditLog.setErrorMsg(errorMsg);
            auditLog.setExecutionTime((int) duration);
            auditLog.setCreatedAt(LocalDateTime.now());

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("保存审计日志失败", e);
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

    /**
     * 获取当前用户名
     */
    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }

    /**
     * 获取请求对象
     */
    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }
}