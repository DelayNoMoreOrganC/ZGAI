package com.lawfirm.repository;

import com.lawfirm.entity.Party;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 当事人Repository
 */
@Repository
public interface PartyRepository extends JpaRepository<Party, Long> {

    /**
     * 根据案件ID查找当事人列表
     */
    List<Party> findByCaseId(Long caseId);

    /**
     * 根据案件ID查找未删除的当事人列表
     */
    List<Party> findByCaseIdAndDeletedFalse(Long caseId);

    /**
     * 根据当事人属性查找当事人
     */
    List<Party> findByPartyRole(String partyRole);

    /**
     * 根据名称查找未删除的当事人
     */
    List<Party> findByNameAndDeletedFalse(String name);

    /**
     * 查找所有未删除的当事人（搜索优化）
     */
    List<Party> findByDeletedFalse();

    /**
     * 全局搜索当事人（性能优化）
     */
    @Query("SELECT p FROM Party p WHERE p.deleted = false AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "p.phone LIKE CONCAT('%', :keyword, '%') OR " +
           "p.idCard LIKE CONCAT('%', :keyword, '%'))")
    Page<Party> searchParties(@Param("keyword") String keyword, Pageable pageable);
}
