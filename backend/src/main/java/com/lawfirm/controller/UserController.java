package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.UserCreateRequest;
import com.lawfirm.dto.UserDTO;
import com.lawfirm.dto.UserOptionDTO;
import com.lawfirm.dto.UserUpdateRequest;
import com.lawfirm.service.UserService;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.exception.BusinessException;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.util.Result;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('USER_EDIT')")
    @AuditLog(value = "创建用户", operationType = "CREATE", logParams = false)
    public Result<UserDTO> createUser(@Valid @RequestBody UserCreateRequest request) {
        try {
            UserDTO result = userService.createUser(request);
            return Result.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建用户失败", e);
            return Result.error("创建用户失败");
        }
    }

    /**
     * 更新用户
     * PUT /api/user/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    @AuditLog(value = "更新用户", operationType = "UPDATE", logParams = false)
    public Result<UserDTO> updateUser(@PathVariable Long id,
                                     @Valid @RequestBody UserUpdateRequest request) {
        try {
            UserDTO result = userService.updateUser(id, request);
            return Result.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新用户失败", e);
            return Result.error("更新用户失败");
        }
    }

    /**
     * 删除用户
     * DELETE /api/user/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    @AuditLog(value = "删除用户", operationType = "DELETE", logParams = false)
    public Result<Void> deleteUser(@PathVariable Long id) {
        try {
            requireDifferentUser(id, "不能删除当前登录账号");
            userService.deleteUser(id);
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return Result.error("删除用户失败");
        }
    }

    /**
     * 获取用户列表
     * GET /api/user
     */
    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public Result<Page<UserDTO>> getUserList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer status) {
        try {
            Page<UserDTO> result = userService.getUserList(page, size, keyword, departmentId, status);
            return Result.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return Result.error("获取用户列表失败");
        }
    }

    /**
     * 获取用户详情
     * GET /api/user/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public Result<UserDTO> getUserDetail(@PathVariable Long id) {
        try {
            UserDTO result = userService.getUserDetail(id);
            return Result.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            return Result.error("获取用户详情失败");
        }
    }

    /**
     * 获取业务表单可选择的在职员工最小目录。
     */
    @GetMapping("/options")
    @PreAuthorize("isAuthenticated()")
    public Result<List<UserOptionDTO>> getUserOptions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "300") int size) {
        return Result.success(userService.getUserOptions(keyword, departmentId, size));
    }

    /**
     * 启用/禁用用户
     * PUT /api/user/{id}/status
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    @AuditLog(value = "变更用户状态", operationType = "UPDATE", logParams = false)
    public Result<Void> toggleUserStatus(@PathVariable Long id,
                                        @RequestParam Integer status) {
        try {
            requireDifferentUser(id, "不能停用当前登录账号");
            userService.toggleUserStatus(id, status);
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("切换用户状态失败", e);
            return Result.error("切换用户状态失败");
        }
    }

    /**
     * 重置密码
     * PUT /api/user/{id}/reset-password
     */
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    @AuditLog(value = "重置用户密码", operationType = "UPDATE", logParams = false)
    public Result<Void> resetPassword(@PathVariable Long id,
                                     @RequestBody Map<String, String> params) {
        try {
            requireDifferentUser(id, "当前登录账号请在个人中心修改密码");
            String newPassword = params.get("newPassword");
            userService.resetPassword(id, newPassword);
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("重置密码失败", e);
            return Result.error("重置密码失败");
        }
    }

    /**
     * 修改密码
     * PUT /api/user/change-password
     */
    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "修改本人密码", operationType = "UPDATE", logParams = false)
    public Result<Void> changePassword(@RequestBody Map<String, String> params) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            String oldPassword = params.get("oldPassword");
            String newPassword = params.get("newPassword");
            userService.changePassword(userId, oldPassword, newPassword);
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return Result.error("修改密码失败");
        }
    }

    /**
     * 分配角色
     * PUT /api/user/{id}/roles
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_EDIT')")
    @AuditLog(value = "分配用户角色", operationType = "UPDATE", logParams = false)
    public Result<Void> assignRoles(@PathVariable Long id,
                                   @RequestBody Map<String, List<Long>> params) {
        try {
            requireDifferentUser(id, "不能调整当前登录账号的角色");
            List<Long> roleIds = params.get("roleIds");
            userService.assignRoles(id, roleIds);
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("分配角色失败", e);
            return Result.error("分配角色失败");
        }
    }

    private void requireDifferentUser(Long targetUserId, String message) {
        if (targetUserId != null && targetUserId.equals(securityUtils.getCurrentUserId())) {
            throw new InvalidParameterException("userId", message);
        }
    }
}
