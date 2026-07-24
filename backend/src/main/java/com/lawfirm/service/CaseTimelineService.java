package com.lawfirm.service;

import com.lawfirm.dto.CaseTimelineDTO;
import com.lawfirm.entity.CaseTimeline;
import com.lawfirm.entity.User;
import com.lawfirm.repository.CaseTimelineRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 案件动态服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseTimelineService {

    private final CaseTimelineRepository caseTimelineRepository;
    private final UserRepository userRepository;

    /**
     * 创建案件动态
     */
    public CaseTimeline create(CaseTimelineDTO dto, Long caseId, Long operatorId) {
        CaseTimeline timeline = new CaseTimeline();
        BeanUtils.copyProperties(dto, timeline);
        timeline.setCaseId(caseId);
        timeline.setOperatorId(operatorId);

        // 处理@提及
        if (dto.getMentionIds() != null && !dto.getMentionIds().isEmpty()) {
            timeline.setMentions(String.join(",", dto.getMentionIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList())));
        }

        return caseTimelineRepository.save(timeline);
    }

    /**
     * 创建系统自动动态
     */
    public CaseTimeline createSystemTimeline(Long caseId, String actionType, String content) {
        return createSystemTimeline(caseId, actionType, content, 0L);
    }

    /**
     * 创建带实际操作账号的案件日志。
     */
    public CaseTimeline createSystemTimeline(Long caseId, String actionType, String content, Long operatorId) {
        CaseTimeline timeline = new CaseTimeline();
        timeline.setCaseId(caseId);
        timeline.setActionType(actionType);
        timeline.setActionContent(content);
        timeline.setOperatorId(operatorId == null ? 0L : operatorId);
        timeline.setIsComment(false);
        return caseTimelineRepository.save(timeline);
    }

    /**
     * 添加评论
     */
    public CaseTimeline addComment(Long caseId, String content, Long userId, Long parentId) {
        CaseTimelineDTO dto = new CaseTimelineDTO();
        dto.setActionType("COMMENT");
        dto.setActionContent(content);
        dto.setIsComment(true);
        dto.setParentId(parentId);
        return create(dto, caseId, userId);
    }

    /**
     * 删除动态（逻辑删除）
     */
    @Transactional
    public void delete(Long id) {
        CaseTimeline timeline = caseTimelineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("案件动态不存在"));
        timeline.setDeleted(true);
        caseTimelineRepository.save(timeline);
    }

    /**
     * 根据ID查询
     */
    public CaseTimeline getById(Long id) {
        return caseTimelineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("案件动态不存在"));
    }

    /**
     * 根据案件ID查询所有动态
     */
    public List<CaseTimeline> getByCaseId(Long caseId) {
        return enrichOperators(caseTimelineRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId));
    }

    /**
     * 根据案件ID查询评论
     */
    public List<CaseTimeline> getCommentsByCaseId(Long caseId) {
        return enrichOperators(caseTimelineRepository.findByCaseIdAndIsCommentTrueAndDeletedFalseOrderByCreatedAtDesc(caseId));
    }

    /**
     * 根据案件ID查询非评论动态
     */
    public List<CaseTimeline> getSystemTimelineByCaseId(Long caseId) {
        return enrichOperators(caseTimelineRepository.findByCaseIdAndIsCommentFalseAndDeletedFalseOrderByCreatedAtDesc(caseId));
    }

    /**
     * 查询评论的回复
     */
    public List<CaseTimeline> getReplies(Long parentId) {
        return enrichOperators(caseTimelineRepository.findByParentIdAndDeletedFalseOrderByCreatedAtAsc(parentId));
    }

    /**
     * 统计案件评论数
     */
    public long countComments(Long caseId) {
        return caseTimelineRepository.countCommentsByCaseId(caseId);
    }

    /**
     * 统计案件动态数
     */
    public long countTimelines(Long caseId) {
        return caseTimelineRepository.countByCaseId(caseId);
    }

    private List<CaseTimeline> enrichOperators(List<CaseTimeline> timelines) {
        List<Long> userIds = timelines.stream()
                .map(CaseTimeline::getOperatorId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        timelines.forEach(item -> {
            if (item.getOperatorId() == null || item.getOperatorId() == 0L) {
                item.setOperatorName("系统");
                return;
            }
            User user = users.get(item.getOperatorId());
            item.setOperatorName(user == null
                    ? "已停用账号（ID " + item.getOperatorId() + "）"
                    : user.getRealName() + "（" + user.getUsername() + "）");
        });
        return timelines;
    }
}
