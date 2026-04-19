package com.lawfirm.controller;

import com.lawfirm.dto.UserCreateRequest;
import com.lawfirm.dto.UserDTO;
import com.lawfirm.dto.UserUpdateRequest;
import com.lawfirm.service.UserService;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.Result;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    /**
     * 创建用户
     * POST /api/user
     */
    @PostMapping
    public Result<UserDTO> createUser(@Valid @RequestBody UserCreateRequest request) {
        try {
            UserDTO result = userService.createUser(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建用户失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户
     * PUT /api/user/{id}
     */
    @PutMapping("/{id}")
    public Result<UserDTO> updateUser(@PathVariable Long id,
                                     @Valid @RequestBody UserUpdateRequest request) {
        try {
            UserDTO result = userService.updateUser(id, request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("更新用户失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除用户
     * DELETE /api/user/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户列表
     * GET /api/user
     */
    @GetMapping
    public Result<Page<UserDTO>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer status) {
        try {
            Page<UserDTO> result = userService.getUserList(page, size, keyword, departmentId, status);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户详情
     * GET /api/user/{id}
     */
    @GetMapping("/{id}")
    public Result<UserDTO> getUserDetail(@PathVariable Long id) {
        try {
            UserDTO result = userService.getUserDetail(id);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 启用/禁用用户
     * PUT /api/user/{id}/status
     */
    @PutMapping("/{id}/status")
    public Result<Void> toggleUserStatus(@PathVariable Long id,
                                        @RequestParam Integer status) {
        try {
            userService.toggleUserStatus(id, status);
            return Result.success();
        } catch (Exception e) {
            log.error("切换用户状态失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 重置密码
     * PUT /api/user/{id}/reset-password
     */
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                     @RequestBody Map<String, String> params) {
        try {
            String newPassword = params.get("newPassword");
            userService.resetPassword(id, newPassword);
            return Result.success();
        } catch (Exception e) {
            log.error("重置密码失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改密码
     * PUT /api/user/change-password
     */
    @PutMapping("/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> params) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            String oldPassword = params.get("oldPassword");
            String newPassword = params.get("newPassword");
            userService.changePassword(userId, oldPassword, newPassword);
            return Result.success();
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分配角色
     * PUT /api/user/{id}/roles
     */
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id,
                                   @RequestBody Map<String, List<Long>> params) {
        try {
            List<Long> roleIds = params.get("roleIds");
            userService.assignRoles(id, roleIds);
            return Result.success();
        } catch (Exception e) {
            log.error("分配角色失败", e);
            return Result.error(e.getMessage());
        }
    }
}
