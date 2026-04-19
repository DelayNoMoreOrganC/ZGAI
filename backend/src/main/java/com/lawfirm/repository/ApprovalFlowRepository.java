package com.lawfirm.repository;

import com.lawfirm.entity.ApprovalFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审批流程记录Repository
 */
@Repository
public interface ApprovalFlowRepository extends JpaRepository<ApprovalFlow, Long> {

    /**
     * 根据审批单ID查找流程记录
     */
    List<ApprovalFlow> findByApprovalIdOrderByActionTimeAsc(Long approvalId);

    /**
     * 根据审批人查找流程记录
     */
    List<ApprovalFlow> findByApproverIdOrderByActionTimeDesc(Long approverId);
}
