package com.lawfirm.controller;

import com.lawfirm.entity.User;
import com.lawfirm.entity.Role;
import com.lawfirm.entity.UserRole;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 测试数据初始化Controller（仅开发环境）
 */
@RestController
@RequestMapping("/init")
@RequiredArgsConstructor
public class InitDataController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建测试管理员账号
     */
    @PostMapping("/admin")
    public String createAdminUser() {
        // 检查是否已存在
        if (userRepository.existsByUsername("admin")) {
            return "测试账号已存在，请直接登录";
        }

        // 创建管理员账号
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRealName("系统管理员");
        admin.setEmail("admin@lawfirm.com");
        admin.setPhone("13800138000");
        admin.setStatus(1);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());

        admin = userRepository.save(admin);

        // 分配管理员角色
        Role adminRole = roleRepository.findByRoleCode("ADMIN").orElse(null);
        if (adminRole == null) {
            // 如果ADMIN角色不存在，创建一个
            adminRole = new Role();
            adminRole.setRoleCode("ADMIN");
            adminRole.setRoleName("系统管理员");
            adminRole.setDescription("系统管理员角色");
            adminRole.setCreatedAt(LocalDateTime.now());
            adminRole.setUpdatedAt(LocalDateTime.now());
            adminRole.setDeleted(false);
            adminRole = roleRepository.save(adminRole);
        }

        // 创建用户角色关联
        UserRole userRole = new UserRole();
        userRole.setUserId(admin.getId());
        userRole.setRoleId(adminRole.getId());
        userRole.setCreatedAt(LocalDateTime.now());
        userRoleRepository.save(userRole);

        return "测试账号创建成功！\n账号：admin\n密码：admin123";
    }

    /**
     * 检查测试账号是否存在
     */
    @GetMapping("/check")
    public String checkAdminUser() {
        boolean exists = userRepository.existsByUsername("admin");
        return exists ? "测试账号已存在，请直接登录" : "测试账号不存在，请调用 POST /api/init/admin 创建";
    }
}
