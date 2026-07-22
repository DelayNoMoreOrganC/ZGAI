package com.lawfirm.controller;

import com.lawfirm.dto.AuditLogDTO;
import com.lawfirm.entity.AuditLog;
import com.lawfirm.entity.User;
import com.lawfirm.repository.AuditLogRepository;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Read-only administrative audit log. Sensitive request parameters and internal errors stay server-side.
 */
@RestController
@RequestMapping("audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @GetMapping
    public Result<Page<AuditLogDTO>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<AuditLog> specification = auditSpecification(module, operation, userId, startDate, endDate);
        Page<AuditLog> logs = auditLogRepository.findAll(specification, pageable);
        Map<Long, String> userNames = loadUserNames(logs.getContent());
        return Result.success(logs.map(log -> toDTO(log, userNames)));
    }

    @GetMapping("/{id}")
    public Result<AuditLogDTO> getAuditLogById(@PathVariable Long id) {
        return auditLogRepository.findById(id)
                .map(log -> {
                    Map<Long, String> names = loadUserNames(Collections.singletonList(log));
                    return Result.success(toDTO(log, names));
                })
                .orElse(Result.notFound("审计日志不存在"));
    }

    @GetMapping("/modules")
    public Result<List<String>> getModules() {
        return Result.success(auditLogRepository.findAll().stream()
                .map(AuditLog::getModule)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .collect(Collectors.toList()));
    }

    @GetMapping("/operations")
    public Result<List<String>> getOperations() {
        return Result.success(auditLogRepository.findAll().stream()
                .map(AuditLog::getOperation)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .collect(Collectors.toList()));
    }

    @GetMapping("/stats")
    public Result<Map<String, Long>> getAuditLogStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<AuditLog> logs = auditLogRepository.findAll();
        long today = logs.stream()
                .filter(log -> log.getCreatedAt() != null && !log.getCreatedAt().isBefore(todayStart))
                .count();
        long failed = logs.stream()
                .filter(log -> log.getStatus() != null && log.getStatus() != 1)
                .count();
        return Result.success(Map.of(
                "total", (long) logs.size(),
                "today", today,
                "failed", failed
        ));
    }

    private Specification<AuditLog> auditSpecification(String module, String operation, Long userId,
                                                         LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(module)) {
                predicates.add(cb.equal(root.get("module"), module.trim()));
            }
            if (StringUtils.hasText(operation)) {
                predicates.add(cb.equal(root.get("operation"), operation.trim()));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), endDate.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Map<Long, String> loadUserNames(List<AuditLog> logs) {
        List<Long> userIds = logs.stream()
                .map(AuditLog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getRealName, (left, right) -> left));
    }

    private AuditLogDTO toDTO(AuditLog log, Map<Long, String> userNames) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(log.getId());
        dto.setUserId(log.getUserId());
        dto.setUserName(userNames.getOrDefault(log.getUserId(), "系统"));
        dto.setModule(log.getModule());
        dto.setOperation(log.getOperation());
        dto.setMethod(log.getMethod());
        dto.setIp(log.getIp());
        dto.setStatus(log.getStatus());
        dto.setExecutionTime(log.getExecutionTime());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
