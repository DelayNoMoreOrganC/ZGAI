package com.lawfirm.repository;

import com.lawfirm.entity.AILog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * AI日志Repository
 */
@Repository
public interface AILogRepository extends JpaRepository<AILog, Long> {

    /**
     * 查询用户的AI使用日志
     */
    Page<AILog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 查询案件的AI使用日志
     */
    Page<AILog> findByCaseIdOrderByCreatedAtDesc(Long caseId, Pageable pageable);

    /**
     * 查询所有AI使用日志（按创建时间倒序）
     */
    Page<AILog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
