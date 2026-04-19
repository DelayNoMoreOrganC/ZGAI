package com.lawfirm.repository;

import com.lawfirm.entity.CaseProcedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 案件程序Repository
 */
@Repository
public interface CaseProcedureRepository extends JpaRepository<CaseProcedure, Long> {

    /**
     * 根据案件ID查找程序列表
     */
    List<CaseProcedure> findByCaseId(Long caseId);

    /**
     * 根据案件ID查找未删除的程序列表（按创建时间倒序）
     */
    List<CaseProcedure> findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(Long caseId);
}
