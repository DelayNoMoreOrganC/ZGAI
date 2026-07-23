package com.lawfirm.service;

import com.lawfirm.entity.Department;
import com.lawfirm.entity.Permission;
import com.lawfirm.entity.Role;
import com.lawfirm.entity.RolePermission;
import com.lawfirm.entity.User;
import com.lawfirm.entity.UserRole;
import com.lawfirm.repository.DepartmentRepository;
import com.lawfirm.repository.PermissionRepository;
import com.lawfirm.repository.RolePermissionRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 根据员工档案工作簿同步组织架构、员工身份和核心权限。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeePermissionSyncService {

    private static final String ROOT_DEPARTMENT = "广东至高律师事务所";

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${employee-sync.enabled:false}")
    private boolean syncEnabled;

    @Value("${employee-sync.workbook-path:}")
    private String workbookPath;

    @Value("${permission-bootstrap.client-all-view-users:}")
    private String clientAllViewUsers;

    @Value("${permission-bootstrap.invoice-processors:}")
    private String invoiceProcessors;

    @Value("${permission-bootstrap.case-filing-managers:}")
    private String caseFilingManagers;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncFromEmployeeWorkbook() {
        Map<String, Role> roles;
        try {
            roles = syncRolesAndPermissions();
            syncConfiguredCapabilityRoles(roles);
            log.info("系统角色权限基线已校准");
        } catch (Exception e) {
            log.error("系统角色权限基线校准失败：{}", e.getMessage(), e);
            return;
        }
        if (!syncEnabled) {
            log.info("员工档案自动同步未启用");
            return;
        }
        if (!StringUtils.hasText(workbookPath)) {
            log.warn("员工档案自动同步已启用，但未设置 EMPLOYEE_SYNC_WORKBOOK");
            return;
        }
        Path sourcePath = Path.of(workbookPath);
        if (!Files.exists(sourcePath)) {
            log.info("员工权限同步文件不存在，跳过：{}", sourcePath);
            return;
        }

        try (InputStream inputStream = Files.newInputStream(sourcePath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter();
            Map<String, OrgRow> orgRows = readOrgRows(workbook, formatter);
            List<EmployeeRow> employees = readEmployeeRows(workbook, formatter);

            Map<String, Department> departments = syncDepartments(orgRows, employees);
            int userCount = syncUsers(employees, departments, roles);
            syncDepartmentLeaders(orgRows, departments);
            syncConfiguredCapabilityRoles(roles);

            log.info("员工权限同步完成：部门 {} 个，员工 {} 人，来源 {}", departments.size(), userCount, sourcePath);
        } catch (Exception e) {
            log.error("员工权限同步失败：{}", e.getMessage(), e);
        }
    }

    private Map<String, OrgRow> readOrgRows(Workbook workbook, DataFormatter formatter) {
        Sheet sheet = workbook.getSheet("组织架构");
        if (sheet == null) {
            return Collections.emptyMap();
        }
        Map<String, OrgRow> rows = new LinkedHashMap<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            String name = cell(row, 1, formatter);
            if (!StringUtils.hasText(name)) {
                continue;
            }
            rows.put(normalizeDepartmentName(name), new OrgRow(
                    normalizeDepartmentName(name),
                    cell(row, 2, formatter),
                    cell(row, 3, formatter)));
        }
        return rows;
    }

    private List<EmployeeRow> readEmployeeRows(Workbook workbook, DataFormatter formatter) {
        Sheet sheet = workbook.getSheet("员工档案");
        if (sheet == null) {
            return Collections.emptyList();
        }
        Map<String, Integer> headers = readHeaders(sheet.getRow(0), formatter);
        List<EmployeeRow> rows = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            String name = cell(row, headers.get("员工姓名"), formatter);
            if (!StringUtils.hasText(name)) {
                continue;
            }
            rows.add(new EmployeeRow(
                    name,
                    normalizeDepartmentName(cell(row, headers.get("*归属部门"), formatter)),
                    cell(row, headers.get("身份"), formatter),
                    cell(row, headers.get("*邮箱"), formatter),
                    normalizePhone(cell(row, headers.get("*手机号"), formatter)),
                    cell(row, headers.get("人员状态"), formatter)
            ));
        }
        return rows;
    }

    private Map<String, Integer> readHeaders(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> headers = new HashMap<>();
        if (headerRow == null) {
            return headers;
        }
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            String header = cell(headerRow, i, formatter).replace(" ", "");
            if (StringUtils.hasText(header)) {
                headers.put(header, i);
            }
        }
        return headers;
    }

    private Map<String, Department> syncDepartments(Map<String, OrgRow> orgRows, List<EmployeeRow> employees) {
        Department root = ensureDepartment(ROOT_DEPARTMENT, 0L, "律所根部门");
        Map<String, Department> departments = new LinkedHashMap<>();
        departments.put(ROOT_DEPARTMENT, root);

        for (String departmentName : orgRows.keySet()) {
            Long parentId = ROOT_DEPARTMENT.equals(departmentName) ? 0L : root.getId();
            departments.put(departmentName, ensureDepartment(departmentName, parentId, "员工档案同步"));
        }
        for (EmployeeRow employee : employees) {
            if (StringUtils.hasText(employee.departmentName)) {
                departments.putIfAbsent(employee.departmentName,
                        ensureDepartment(employee.departmentName, root.getId(), "员工档案同步"));
            }
        }
        return departments;
    }

    private Department ensureDepartment(String name, Long parentId, String description) {
        Department department = departmentRepository.findByDeptName(name);
        if (department == null) {
            department = new Department();
            department.setDeptName(name);
        }
        department.setParentId(parentId);
        department.setDescription(description);
        department.setDeleted(false);
        return departmentRepository.save(department);
    }

    private Map<String, Role> syncRolesAndPermissions() {
        Map<String, Permission> permissions = permissionDefinitions().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> ensurePermission(entry.getKey(), entry.getValue())));

        Map<String, Role> roles = roleDefinitions().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> ensureRole(entry.getKey(), entry.getValue())));

        assignPermissions(roles.get("MANAGER"), permissions, allPermissions());
        assignPermissions(roles.get("DEPT_HEAD"), permissions, employeePermissions());
        assignPermissions(roles.get("LAWYER"), permissions, employeePermissions());
        assignPermissions(roles.get("ASSISTANT"), permissions, employeePermissions());
        assignPermissions(roles.get("LAWYER_ASSISTANT"), permissions, employeePermissions());
        assignPermissions(roles.get("TRAINEE"), permissions, employeePermissions());
        assignPermissions(roles.get("FINANCE"), permissions, financePermissions());
        assignPermissions(roles.get("ADMINISTRATIVE"), permissions, administrativePermissions());
        assignPermissions(roles.get("ADMINISTRATIVE1"), permissions,
                concat(administrativePermissions(), "CASE_FILING_MANAGE"));
        assignPermissions(roles.get("ADMINISTRATIVE2"), permissions, administrativePermissions());
        assignPermissions(roles.get("CLIENT_AUDITOR"), permissions,
                Arrays.asList("CLIENT_VIEW", "CLIENT_VIEW_ALL"));
        assignPermissions(roles.get("INVOICE_PROCESSOR"), permissions,
                Arrays.asList("FINANCE_VIEW", "FINANCE_EDIT", "INVOICE_PROCESS", "CLIENT_VIEW_ALL"));
        assignPermissions(roles.get("CASE_FILING_ADMIN"), permissions,
                Arrays.asList("CASE_VIEW", "CASE_EDIT", "APPROVAL_VIEW", "APPROVAL_EDIT",
                        "CASE_FILING_REVIEW", "CASE_FILING_MANAGE", "CLIENT_VIEW_ALL"));

        return roles;
    }

    private Map<String, String> permissionDefinitions() {
        Map<String, String> definitions = new LinkedHashMap<>();
        definitions.put("CASE_CREATE", "新建立案申请");
        definitions.put("CASE_VIEW", "查看案件");
        definitions.put("CASE_EDIT", "编辑案件");
        definitions.put("CASE_DELETE", "删除案件");
        definitions.put("CASE_ARCHIVE", "案件归档");
        definitions.put("CASE_ARCHIVE_REVIEW", "行政复核归档");
        definitions.put("CASE_IMPORT", "导入案件");
        definitions.put("CASE_EXPORT", "导出案件");
        definitions.put("CLIENT_CREATE", "新建客户");
        definitions.put("CLIENT_VIEW", "查看客户");
        definitions.put("CLIENT_VIEW_ALL", "查看全所客户");
        definitions.put("CLIENT_EDIT", "编辑客户");
        definitions.put("CLIENT_DELETE", "删除客户");
        definitions.put("CLIENT_IMPORT", "导入客户");
        definitions.put("CLIENT_EXPORT", "导出客户");
        definitions.put("DOCUMENT_VIEW", "查看文档");
        definitions.put("DOCUMENT_EDIT", "编辑文档");
        definitions.put("DOCUMENT_DELETE", "删除文档");
        definitions.put("APPROVAL_VIEW", "查看审批");
        definitions.put("APPROVAL_EDIT", "处理审批");
        definitions.put("APPROVAL_DELETE", "删除审批");
        definitions.put("SEAL_APPROVE", "公章用印审批");
        definitions.put("CASE_FILING_REVIEW", "立案行政初审");
        definitions.put("CASE_FILING_FINAL_APPROVE", "立案主任终审");
        definitions.put("CASE_FILING_MANAGE", "修订审批中案件");
        definitions.put("TODO_VIEW", "查看待办");
        definitions.put("TODO_EDIT", "编辑待办");
        definitions.put("TODO_DELETE", "删除待办");
        definitions.put("FINANCE_VIEW", "查看财务");
        definitions.put("FINANCE_EDIT", "处理财务");
        definitions.put("INVOICE_PROCESS", "处理开票申请");
        definitions.put("USER_VIEW", "查看用户");
        definitions.put("USER_EDIT", "编辑用户");
        definitions.put("ROLE_VIEW", "查看角色");
        definitions.put("ROLE_EDIT", "编辑角色");
        definitions.put("AI_CONFIG", "配置AI服务");
        definitions.put("SYSTEM_CONFIG", "系统配置");
        definitions.put("WORK_REPORT_REVIEW", "审核工作报告");
        definitions.put("KNOWLEDGE_DELETE", "删除知识库");
        definitions.put("KNOWLEDGE_MANAGE", "审核与发布知识库");
        definitions.put("STATISTICS_VIEW", "查看统计");
        definitions.put("STATISTICS_EXPORT", "导出统计");
        return definitions;
    }

    private Map<String, String> roleDefinitions() {
        Map<String, String> definitions = new LinkedHashMap<>();
        definitions.put("MANAGER", "主任");
        definitions.put("DEPT_HEAD", "主管");
        definitions.put("LAWYER", "律师");
        definitions.put("ASSISTANT", "助理");
        definitions.put("LAWYER_ASSISTANT", "律师助理");
        definitions.put("TRAINEE", "实习律师");
        definitions.put("FINANCE", "财务管理");
        definitions.put("ADMINISTRATIVE", "行政管理");
        definitions.put("ADMINISTRATIVE1", "行政管理1");
        definitions.put("ADMINISTRATIVE2", "行政管理2");
        definitions.put("CLIENT_AUDITOR", "客户全库查看");
        definitions.put("INVOICE_PROCESSOR", "开票处理人");
        definitions.put("CASE_FILING_ADMIN", "立案行政管理");
        return definitions;
    }

    private void syncConfiguredCapabilityRoles(Map<String, Role> roles) {
        assignConfiguredRole(clientAllViewUsers, roles.get("CLIENT_AUDITOR"));
        assignConfiguredRole(invoiceProcessors, roles.get("INVOICE_PROCESSOR"));
        assignConfiguredRole(caseFilingManagers, roles.get("CASE_FILING_ADMIN"));
    }

    private void assignConfiguredRole(String configuredUsers, Role role) {
        if (!StringUtils.hasText(configuredUsers) || role == null) {
            return;
        }
        Arrays.stream(configuredUsers.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(account -> findUser(account).ifPresent(user -> {
                    boolean assigned = userRoleRepository.findByUserId(user.getId()).stream()
                            .anyMatch(userRole -> role.getId().equals(userRole.getRoleId()));
                    if (!assigned) {
                        UserRole userRole = new UserRole();
                        userRole.setUserId(user.getId());
                        userRole.setRoleId(role.getId());
                        userRoleRepository.save(userRole);
                    }
                }));
    }

    private Role ensureRole(String code, String name) {
        Role role = roleRepository.findByRoleCode(code).orElseGet(Role::new);
        role.setRoleCode(code);
        role.setRoleName(name);
        role.setDescription("员工档案权限同步");
        role.setDeleted(false);
        return roleRepository.save(role);
    }

    private Permission ensurePermission(String code, String name) {
        Permission permission = permissionRepository.findByPermissionCode(code).orElseGet(Permission::new);
        permission.setPermissionCode(code);
        permission.setPermissionName(name);
        permission.setResourceType("BUTTON");
        permission.setParentId(0L);
        permission.setDeleted(false);
        return permissionRepository.save(permission);
    }

    private void assignPermissions(Role role, Map<String, Permission> permissions, List<String> permissionCodes) {
        if (role == null) {
            return;
        }
        rolePermissionRepository.deleteByRoleId(role.getId());
        rolePermissionRepository.flush();
        Set<Long> permissionIds = permissionCodes.stream()
                .map(permissions::get)
                .filter(Objects::nonNull)
                .map(Permission::getId)
                .collect(Collectors.toCollection(HashSet::new));
        for (Long permissionId : permissionIds) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(role.getId());
            rolePermission.setPermissionId(permissionId);
            rolePermissionRepository.save(rolePermission);
        }
    }

    private int syncUsers(List<EmployeeRow> employees, Map<String, Department> departments, Map<String, Role> roles) {
        int count = 0;
        for (EmployeeRow employee : employees) {
            User user = findUser(employee.name).orElseGet(User::new);
            boolean newUser = user.getId() == null;
            user.setUsername(employee.name);
            user.setRealName(employee.name);
            user.setEmail(sanitizeEmail(employee.email));
            user.setPhone(StringUtils.hasText(employee.phone) ? employee.phone : null);
            Department department = departments.get(employee.departmentName);
            user.setDepartmentId(department == null ? null : department.getId());
            user.setPosition(employee.position);
            user.setStatus("在职".equals(employee.status) || !StringUtils.hasText(employee.status) ? 1 : 0);
            user.setDeleted(false);
            if (newUser) {
                user.setPassword(passwordEncoder.encode(defaultPassword(employee.phone)));
                user.setMustChangePassword(true);
            }
            user = userRepository.save(user);
            assignUserRoles(user, employee.position, roles);
            count++;
        }
        return count;
    }

    private Optional<User> findUser(String name) {
        Optional<User> byUsername = userRepository.findByUsername(name);
        if (byUsername.isPresent()) {
            return byUsername;
        }
        return userRepository.findAll().stream()
                .filter(user -> name.equals(user.getRealName()))
                .findFirst();
    }

    private void assignUserRoles(User user, String position, Map<String, Role> roles) {
        if (user == null || "admin".equals(user.getUsername())) {
            return;
        }
        userRoleRepository.deleteByUserId(user.getId());
        userRoleRepository.flush();
        for (String roleCode : roleCodesForPosition(position)) {
            Role role = roles.get(roleCode);
            if (role == null) {
                continue;
            }
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            userRoleRepository.save(userRole);
        }
    }

    List<String> roleCodesForPosition(String position) {
        String normalizedPosition = position == null ? "" : position.trim();
        if ("主任".equals(normalizedPosition)) {
            return Arrays.asList("MANAGER", "LAWYER");
        }
        if ("主管".equals(normalizedPosition) || "部门主管".equals(normalizedPosition)) {
            return Arrays.asList("DEPT_HEAD", "LAWYER");
        }
        if ("律师".equals(normalizedPosition)) {
            return Collections.singletonList("LAWYER");
        }
        if ("律师助理".equals(normalizedPosition)) {
            return Arrays.asList("LAWYER_ASSISTANT", "ASSISTANT");
        }
        if ("实习律师".equals(normalizedPosition)) {
            return Arrays.asList("TRAINEE", "ASSISTANT");
        }
        if ("助理".equals(normalizedPosition)) {
            return Collections.singletonList("ASSISTANT");
        }
        if ("财务管理".equals(normalizedPosition)
                || "财务".equals(normalizedPosition)
                || "出纳".equals(normalizedPosition)) {
            return Collections.singletonList("FINANCE");
        }
        if ("行政管理1".equals(normalizedPosition)) {
            return Arrays.asList("ADMINISTRATIVE", "ADMINISTRATIVE1");
        }
        if ("行政管理2".equals(normalizedPosition)) {
            return Arrays.asList("ADMINISTRATIVE", "ADMINISTRATIVE2");
        }
        if ("行政管理".equals(normalizedPosition) || "行政".equals(normalizedPosition)) {
            return Collections.singletonList("ADMINISTRATIVE");
        }
        return Collections.singletonList("ASSISTANT");
    }

    private void syncDepartmentLeaders(Map<String, OrgRow> orgRows, Map<String, Department> departments) {
        for (OrgRow orgRow : orgRows.values()) {
            Department department = departments.get(orgRow.name);
            if (department == null || !StringUtils.hasText(orgRow.leaderName)) {
                continue;
            }
            findUser(orgRow.leaderName).ifPresent(leader -> {
                department.setLeaderId(leader.getId());
                departmentRepository.save(department);
            });
        }
    }

    private String cell(Row row, Integer index, DataFormatter formatter) {
        if (row == null || index == null || index < 0) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(index)).trim();
    }

    private String normalizeDepartmentName(String departmentName) {
        if (!StringUtils.hasText(departmentName)) {
            return "";
        }
        String trimmed = departmentName.trim();
        if ("破产清算部".equals(trimmed)) {
            return "重整与清算部";
        }
        return trimmed;
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return "";
        }
        return phone.replaceAll("[^0-9+\\-]", "");
    }

    private String sanitizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        String normalized = email.replaceAll("\\s+", "");
        if (normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return normalized;
        }
        log.warn("员工档案邮箱格式不正确，已跳过：{}", email);
        return null;
    }

    private String defaultPassword(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        if (digits.length() >= 6) {
            return digits.substring(digits.length() - 6);
        }
        return "123456";
    }

    List<String> employeePermissions() {
        return Arrays.asList(
                "CASE_CREATE", "CASE_VIEW", "CASE_EDIT", "CASE_ARCHIVE",
                "CLIENT_CREATE", "CLIENT_VIEW", "CLIENT_EDIT",
                "DOCUMENT_VIEW", "DOCUMENT_EDIT",
                "APPROVAL_VIEW", "APPROVAL_EDIT",
                "TODO_VIEW", "TODO_EDIT",
                "STATISTICS_VIEW"
        );
    }

    List<String> administrativePermissions() {
        return Arrays.asList(
                "CASE_CREATE", "CASE_VIEW",
                "CLIENT_CREATE", "CLIENT_VIEW", "CLIENT_EDIT",
                "DOCUMENT_VIEW",
                "APPROVAL_VIEW", "APPROVAL_EDIT", "SEAL_APPROVE",
                "CASE_FILING_REVIEW", "CASE_ARCHIVE_REVIEW", "CLIENT_VIEW_ALL",
                "TODO_VIEW", "TODO_EDIT",
                "USER_VIEW", "STATISTICS_VIEW", "WORK_REPORT_REVIEW"
        );
    }

    List<String> financePermissions() {
        return Arrays.asList(
                "CASE_VIEW",
                "CLIENT_VIEW", "CLIENT_VIEW_ALL",
                "DOCUMENT_VIEW",
                "TODO_VIEW", "TODO_EDIT",
                "FINANCE_VIEW", "FINANCE_EDIT", "INVOICE_PROCESS",
                "STATISTICS_VIEW"
        );
    }

    List<String> allPermissions() {
        return new ArrayList<>(permissionDefinitions().keySet());
    }

    private List<String> concat(List<String> base, String... extra) {
        List<String> merged = new ArrayList<>(base);
        merged.addAll(Arrays.asList(extra));
        return merged.stream().distinct().collect(Collectors.toList());
    }

    private static class OrgRow {
        private final String name;
        private final String leaderName;
        @SuppressWarnings("unused")
        private final String memberNames;

        private OrgRow(String name, String leaderName, String memberNames) {
            this.name = name;
            this.leaderName = leaderName;
            this.memberNames = memberNames;
        }
    }

    private static class EmployeeRow {
        private final String name;
        private final String departmentName;
        private final String position;
        private final String email;
        private final String phone;
        private final String status;

        private EmployeeRow(String name, String departmentName, String position, String email, String phone, String status) {
            this.name = name;
            this.departmentName = departmentName;
            this.position = position;
            this.email = email;
            this.phone = phone;
            this.status = status;
        }
    }
}
