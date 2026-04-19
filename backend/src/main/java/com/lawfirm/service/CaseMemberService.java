package com.lawfirm.service;

import com.lawfirm.entity.CaseMember;
import com.lawfirm.repository.CaseMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 案件成员服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseMemberService {

    private final CaseMemberRepository caseMemberRepository;

    /**
     * 添加案件成员
     */
    public CaseMember addMember(Long caseId, Long userId, String memberType) {
        // 检查是否已经是成员
        Optional<CaseMember> existing = caseMemberRepository.findByCaseIdAndUserId(caseId, userId)
                .stream()
                .filter(m -> !m.getDeleted())
                .findFirst();

        if (existing.isPresent()) {
            // 更新成员类型
            CaseMember member = existing.get();
            member.setMemberType(memberType);
            return caseMemberRepository.save(member);
        }

        // 创建新成员
        CaseMember member = new CaseMember();
        member.setCaseId(caseId);
        member.setUserId(userId);
        member.setMemberType(memberType);
        member.setDeleted(false);
        return caseMemberRepository.save(member);
    }

    /**
     * 批量添加案件成员
     */
    @Transactional
    public void addMembers(Long caseId, List<Long> userIds, String memberType) {
        userIds.forEach(userId -> addMember(caseId, userId, memberType));
    }

    /**
     * 移除案件成员（逻辑删除）
     */
    @Transactional
    public void removeMember(Long caseId, Long userId) {
        List<CaseMember> members = caseMemberRepository.findByCaseIdAndUserId(caseId, userId);
        members.forEach(member -> {
            member.setDeleted(true);
            caseMemberRepository.save(member);
        });
    }

    /**
     * 更新成员类型
     */
    public CaseMember updateMemberType(Long caseId, Long userId, String memberType) {
        List<CaseMember> members = caseMemberRepository.findByCaseIdAndUserId(caseId, userId);
        if (members.isEmpty()) {
            throw new RuntimeException("成员不存在");
        }

        CaseMember member = members.get(0);
        member.setMemberType(memberType);
        return caseMemberRepository.save(member);
    }

    /**
     * 根据案件ID查询所有成员
     */
    public List<CaseMember> getByCaseId(Long caseId) {
        return caseMemberRepository.findByCaseIdAndDeletedFalse(caseId);
    }

    /**
     * 根据案件ID和成员类型查询
     */
    public List<CaseMember> getByCaseIdAndType(Long caseId, String memberType) {
        return caseMemberRepository.findByCaseIdAndMemberTypeAndDeletedFalse(caseId, memberType);
    }

    /**
     * 根据用户ID查询参与的所有案件
     */
    public List<CaseMember> getByUserId(Long userId) {
        return caseMemberRepository.findByUserIdAndDeletedFalse(userId);
    }

    /**
     * 检查用户是否是案件成员
     */
    public boolean isMember(Long caseId, Long userId) {
        return caseMemberRepository.existsByCaseIdAndUserId(caseId, userId);
    }

    /**
     * 删除案件的所有成员
     */
    @Transactional
    public void removeAllByCaseId(Long caseId) {
        caseMemberRepository.deleteByCaseId(caseId);
    }
}
