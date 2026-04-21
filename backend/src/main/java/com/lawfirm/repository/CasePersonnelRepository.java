package com.lawfirm.repository;

import com.lawfirm.entity.CasePersonnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 案件承办人员Repository
 */
@Repository
public interface CasePersonnelRepository extends JpaRepository<CasePersonnel, Long> {

    /**
     * 根据案件ID查找所有承办人员
     */
    List<CasePersonnel> findByCaseIdOrderByCreatedAtDesc(Long caseId);

    /**
     * 根据案件ID删除所有承办人员
     */
    void deleteByCaseId(Long caseId);
}
