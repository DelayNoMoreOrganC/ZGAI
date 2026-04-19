package com.lawfirm.controller;

import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/mobile")
@RequiredArgsConstructor
public class MobileController {
    private final SecurityUtils securityUtils;
    
    @GetMapping("/version")
    public Result<Map<String, String>> getVersion() {
        return Result.success(Map.of(
            "version", "1.0.0",
            "platform", "Law Firm Mobile",
            "minVersion", "1.0.0"
        ));
    }
    
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getMobileDashboard() {
        return Result.success(Map.of(
            "pendingTasks", 2,
            "todayEvents", 1,
            "urgentCases", 0
        ));
    }
    
    @GetMapping("/user/profile")
    public Result<Map<String, String>> getUserProfile() {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(Map.of(
            "userId", String.valueOf(userId),
            "username", "Admin",
            "role", "LAWYER"
        ));
    }
}
