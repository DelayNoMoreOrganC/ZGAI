package com.lawfirm.service;

import com.lawfirm.dto.DepartmentCreateRequest;
import com.lawfirm.dto.DepartmentDTO;
import com.lawfirm.entity.Department;
import com.lawfirm.entity.User;
import com.lawfirm.repository.DepartmentRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    /**
     * 创建部门
     */
    @Transactional
    public DepartmentDTO createDepartment(DepartmentCreateRequest request) {
        // 验证父部门是否存在
        if (request.getParentId() != null && request.getParentId() != 0) {
            if (!departmentRepository.existsById(request.getParentId())) {
                throw new RuntimeException("父部门不存在");
            }
        }

        Department department = new Department();
        BeanUtils.copyProperties(request, department);

        department = departmentRepository.save(department);

        return toDTO(department);
    }

    /**
     * 更新部门
     */
    @Transactional
    public DepartmentDTO updateDepartment(Long deptId, DepartmentCreateRequest request) {
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("部门不存在"));

        // 验证父部门
        if (request.getParentId() != null && request.getParentId() != 0) {
            if (request.getParentId().equals(deptId)) {
                throw new RuntimeException("父部门不能是自己");
            }
            if (!departmentRepository.existsById(request.getParentId())) {
                throw new RuntimeException("父部门不存在");
            }
        }

        department.setDeptName(request.getDeptName());
        department.setParentId(request.getParentId());
        department.setLeaderId(request.getLeaderId());
        department.setDescription(request.getDescription());

        department = departmentRepository.save(department);

        return toDTO(department);
    }

    /**
     * 删除部门
     */
    @Transactional
    public void deleteDepartment(Long deptId) {
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("部门不存在"));

        // 检查是否有子部门
        List<Department> children = departmentRepository.findByParentId(deptId);
        if (!children.isEmpty()) {
            throw new RuntimeException("该部门下有子部门，无法删除");
        }

        // 检查是否有用户
        List<User> users = userRepository.findByDepartmentId(deptId);
        if (!users.isEmpty()) {
            throw new RuntimeException("该部门下有用户，无法删除");
        }

        department.setDeleted(true);
        departmentRepository.save(department);
    }

    /**
     * 获取部门列表（树形结构）
     */
    public List<DepartmentDTO> getDepartmentTree() {
        List<Department> allDepartments = departmentRepository.findAll();

        // 先转换为DTO
        List<DepartmentDTO> dtoList = allDepartments.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        // 构建树形结构
        return buildTree(dtoList, 0L);
    }

    /**
     * 获取部门列表（平铺）
     */
    public List<DepartmentDTO> getDepartmentList() {
        List<Department> departments = departmentRepository.findAll();

        return departments.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取部门详情
     */
    public DepartmentDTO getDepartmentDetail(Long deptId) {
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("部门不存在"));

        return toDTO(department);
    }

    // 辅助方法

    private DepartmentDTO toDTO(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        BeanUtils.copyProperties(department, dto);

        // 设置父部门名称
        if (department.getParentId() != null && department.getParentId() != 0) {
            departmentRepository.findById(department.getParentId())
                    .ifPresent(parent -> dto.setParentName(parent.getDeptName()));
        }

        // 设置领导名称
        if (department.getLeaderId() != null) {
            userRepository.findById(department.getLeaderId())
                    .ifPresent(leader -> dto.setLeaderName(leader.getRealName()));
        }

        // 统计用户数量
        List<User> users = userRepository.findByDepartmentId(department.getId());
        dto.setUserCount(users.size());

        return dto;
    }

    /**
     * 构建树形结构
     */
    private List<DepartmentDTO> buildTree(List<DepartmentDTO> allDepartments, Long parentId) {
        List<DepartmentDTO> result = new ArrayList<>();

        for (DepartmentDTO department : allDepartments) {
            if (department.getParentId() != null && department.getParentId().equals(parentId)) {
                // 递归查找子部门
                List<DepartmentDTO> children = buildTree(allDepartments, department.getId());
                department.setChildren(children);
                result.add(department);
            }
        }

        return result;
    }
}
