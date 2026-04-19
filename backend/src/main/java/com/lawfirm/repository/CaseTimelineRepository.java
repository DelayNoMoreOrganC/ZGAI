package com.lawfirm.repository;

import com.lawfirm.entity.CaseTimeline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 案件动态Repository
 */
@Repository
public interface CaseTimelineRepository extends JpaRepository<CaseTimeline, Long> {

    /**
     * 根据案件ID查询所有动态（按时间倒序）
     */
    List<CaseTimeline> findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(Long caseId);

    /**
     * 根据案件ID分页查询动态
     */
    Page<CaseTimeline> findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(Long caseId, Pageable pageable);

    /**
     * 根据案件ID查询评论
     */
    List<CaseTimeline> findByCaseIdAndIsCommentTrueAndDeletedFalseOrderByCreatedAtDesc(Long caseId);

    /**
     * 根据案件ID查询非评论动态
     */
    List<CaseTimeline> findByCaseIdAndIsCommentFalseAndDeletedFalseOrderByCreatedAtDesc(Long caseId);

    /**
     * 根据父级ID查询回复
     */
    List<CaseTimeline> findByParentIdAndDeletedFalseOrderByCreatedAtAsc(Long parentId);

    /**
     * 根据操作人ID查询动态
     */
    List<CaseTimeline> findByOperatorIdAndDeletedFalseOrderByCreatedAtDesc(Long operatorId);

    /**
     * 统计案件评论数
     */
    @Query("SELECT COUNT(ct) FROM CaseTimeline ct WHERE ct.caseId = :caseId AND ct.isComment = true AND ct.deleted = false")
    long countCommentsByCaseId(@Param("caseId") Long caseId);

    /**
     * 统计案件动态数
     */
    @Query("SELECT COUNT(ct) FROM CaseTimeline ct WHERE ct.caseId = :caseId AND ct.deleted = false")
    long countByCaseId(@Param("caseId") Long caseId);
}
