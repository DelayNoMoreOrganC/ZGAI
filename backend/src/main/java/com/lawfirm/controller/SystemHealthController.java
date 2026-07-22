package com.lawfirm.controller;

import com.lawfirm.util.Result;
import com.lawfirm.service.StorageHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 供启动脚本和运维探针使用的轻量健康检查。
 */
@RestController
@RequiredArgsConstructor
public class SystemHealthController {

    private final DataSource dataSource;
    private final Environment environment;
    private final StorageHealthService storageHealthService;

    @GetMapping("/health")
    public ResponseEntity<Result<Map<String, Object>>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", "zgai-backend");
        status.put("time", LocalDateTime.now());
        boolean ready = false;
        try (Connection connection = dataSource.getConnection()) {
            ready = connection.isValid(2);
            status.put("database", ready ? "ready" : "unavailable");
            status.put("status", ready ? "ready" : "degraded");
        } catch (Exception e) {
            status.put("database", "unavailable");
            status.put("status", "degraded");
        }
        Result<Map<String, Object>> body = Result.success(status);
        return ready
                ? ResponseEntity.ok(body)
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @GetMapping("/system/health/details")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public Result<Map<String, Object>> details() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", "zgai-backend");
        result.put("time", LocalDateTime.now());
        result.put("profiles", Arrays.asList(environment.getActiveProfiles()));

        Map<String, Object> database = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            boolean ready = connection.isValid(2);
            database.put("status", ready ? "ready" : "unavailable");
            database.put("product", connection.getMetaData().getDatabaseProductName());
            database.put("version", connection.getMetaData().getDatabaseProductVersion());
        } catch (Exception e) {
            database.put("status", "unavailable");
        }
        result.put("database", database);

        Map<String, Object> storage = storageHealthService.getStorageStatus();
        result.put("storage", storage);
        boolean storageReady = storage.values().stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .allMatch(item -> "ready".equals(item.get("status")));
        result.put("status", "ready".equals(database.get("status")) && storageReady
                ? "ready" : "degraded");
        return Result.success(result);
    }
}
