package com.lawfirm.controller;

import com.lawfirm.dto.UserDTO;
import com.lawfirm.service.UserService;
import com.lawfirm.util.Result;
import com.lawfirm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器 - 兼容前端调用路径
 * 前端调用 /api/admin/users，后端实际是 /api/user
 */
@Slf4j
@RestController
@RequestMapping("admin")
@RequiredArgsConstructor
public class UserControllerCompat {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    /**
     * 获取用户列表
     * GET /api/admin/users
     */
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public Result<org.springframework.data.domain.Page<UserDTO>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            org.springframework.data.domain.Page<UserDTO> result =
                userService.getUserList(page, size, null, null, null);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return Result.error(e.getMessage());
        }
    }
}
