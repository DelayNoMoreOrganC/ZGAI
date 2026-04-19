package com.lawfirm.repository;

import com.lawfirm.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 部门Repository
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * 根据父部门ID查找子部门列表
     */
    List<Department> findByParentId(Long parentId);

    /**
     * 根据部门名称查找部门
     */
    Department findByDeptName(String deptName);
}
