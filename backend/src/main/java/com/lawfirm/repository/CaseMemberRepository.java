package com.lawfirm.repository;

import com.lawfirm.entity.CaseMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 案件成员Repository
 */
@Repository
public interface CaseMemberRepository extends JpaRepository<CaseMember, Long> {

    /**
     * 根据案件ID查询所有成员
     */
    List<CaseMember> findByCaseIdAndDeletedFalse(Long caseId);

    /**
     * 根据案件ID和成员类型查询
     */
    List<CaseMember> findByCaseIdAndMemberTypeAndDeletedFalse(Long caseId, String memberType);

    /**
     * 根据用户ID查询参与的所有案件
     */
    List<CaseMember> findByUserIdAndDeletedFalse(Long userId);

    /**
     * 根据案件ID和用户ID查询
     */
    @Query("SELECT cm FROM CaseMember cm WHERE cm.caseId = :caseId AND cm.userId = :userId AND cm.deleted = false")
    List<CaseMember> findByCaseIdAndUserId(@Param("caseId") Long caseId, @Param("userId") Long userId);

    /**
     * 检查用户是否是案件成员
     */
    @Query("SELECT COUNT(cm) > 0 FROM CaseMember cm WHERE cm.caseId = :caseId AND cm.userId = :userId AND cm.deleted = false")
    boolean existsByCaseIdAndUserId(@Param("caseId") Long caseId, @Param("userId") Long userId);

    /**
     * 删除案件的所有成员
     */
    void deleteByCaseId(Long caseId);
}
