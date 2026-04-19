package com.lawfirm.service;

import com.lawfirm.entity.Notification;
import com.lawfirm.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知服务 - 负责站内通知存储和查询
 * PRD要求：逾期预警 + 案件审限预警通知
 * 采用数据库存储方式，前端通过API轮询获取
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // 通知分类常量
    public static final String CATEGORY_TODO = "待办";
    public static final String CATEGORY_CASE = "案件";
    public static final String CATEGORY_CALENDAR = "日程";
    public static final String CATEGORY_APPROVAL = "审批";
    public static final String CATEGORY_SYSTEM = "系统";

    /**
     * 发送待办逾期通知
     */
    @Transactional
    public void sendTodoOverdueNotification(Long todoId, String title, String dueDate, Long userId) {
        if (userId == null) {
            log.warn("待办逾期通知失败：用户ID为空，todoId={}", todoId);
            return;
        }

        Notification notification = new Notification();
        notification.setReceiverId(userId);
        notification.setTitle("待办逾期预警");
        notification.setContent(String.format("待办事项「%s」将于%s到期，请及时处理", title, dueDate));
        notification.setCategory("TODO_OVERDUE");
        notification.setRelatedId(todoId);
        notification.setRelatedType("Todo");
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
        log.info("待办逾期通知已发送：userId={}, todoId={}", userId, todoId);
    }

    /**
     * 发送案件审限通知
     */
    @Transactional
    public void sendCaseDeadlineNotification(Long caseId, String caseName, String deadline, Long lawyerId) {
        if (lawyerId == null) {
            log.warn("案件审限通知失败：律师ID为空，caseId={}", caseId);
            return;
        }

        Notification notification = new Notification();
        notification.setReceiverId(lawyerId);
        notification.setTitle("案件审限预警");
        notification.setContent(String.format("案件「%s」的审限截止日期为%s，请关注", caseName, deadline));
        notification.setCategory("CASE_DEADLINE");
        notification.setRelatedId(caseId);
        notification.setRelatedType("Case");
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
        log.info("案件审限通知已发送：lawyerId={}, caseId={}", lawyerId, caseId);
    }

    /**
     * 发送紧急待办统计通知（管理员）
     */
    @Transactional
    public void sendUrgentTodoCountNotification(long count) {
        log.warn("当前有{}个待办将在3天内到期，请及时处理", count);
        // 系统级通知可以通过创建一个特殊用户或直接写入日志
        // 这里简化处理，仅记录日志
    }

    /**
     * 发送通用通知
     */
    @Transactional
    public void sendNotification(Long userId, String title, String content, String category, Long relatedId, String relatedType) {
        if (userId == null) {
            log.warn("通知发送失败：用户ID为空");
            return;
        }

        Notification notification = new Notification();
        notification.setReceiverId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setCategory(category);
        notification.setRelatedId(relatedId);
        notification.setRelatedType(relatedType);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
        log.info("通知已发送：userId={}, category={}, title={}", userId, category, title);
    }

    /**
     * 转换Notification为Map
     */
    private Map<String, Object> notificationToMap(Notification notification) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", notification.getId());
        map.put("receiverId", notification.getReceiverId());
        map.put("title", notification.getTitle());
        map.put("content", notification.getContent());
        map.put("category", notification.getCategory());
        map.put("relatedId", notification.getRelatedId());
        map.put("relatedType", notification.getRelatedType());
        map.put("isRead", notification.getIsRead());
        map.put("readTime", notification.getReadTime());
        map.put("createdAt", notification.getCreatedAt());
        return map;
    }

    /**
     * 获取用户通知列表
     */
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getNotificationList(Long userId, int page, int size, String category) {
        Pageable pageable = PageRequest.of(page - 1, size,
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        Page<Notification> notificationPage;
        if (category != null && !category.isEmpty()) {
            notificationPage = notificationRepository.findByReceiverIdAndCategoryOrderByCreatedAtDesc(userId, category, pageable);
        } else {
            notificationPage = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId, pageable);
        }

        return notificationPage.map(this::notificationToMap);
    }

    /**
     * 获取未读通知
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return notifications.stream()
            .map(this::notificationToMap)
            .collect(Collectors.toList());
    }

    /**
     * 获取未读通知数量
     */
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    /**
     * 标记通知为已读
     */
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null && notification.getReceiverId().equals(userId)) {
            notification.setIsRead(true);
            notification.setReadTime(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("通知已标记为已读：notificationId={}, userId={}", notificationId, userId);
        }
    }

    /**
     * 标记所有通知为已读
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notification.setReadTime(LocalDateTime.now());
        }
        notificationRepository.saveAll(unreadNotifications);
        log.info("所有通知已标记为已读：userId={}, count={}", userId, unreadNotifications.size());
    }

    /**
     * 删除通知
     */
    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null && notification.getReceiverId().equals(userId)) {
            notificationRepository.delete(notification);
            log.info("通知已删除：notificationId={}, userId={}", notificationId, userId);
        }
    }

    /**
     * 获取通知分类列表
     */
    public List<Map<String, String>> getCategories() {
        List<String> categories = Arrays.asList(
            CATEGORY_TODO,
            CATEGORY_CASE,
            CATEGORY_CALENDAR,
            CATEGORY_APPROVAL,
            CATEGORY_SYSTEM
        );

        return categories.stream()
            .map(cat -> {
                Map<String, String> map = new HashMap<>();
                map.put("value", cat);
                map.put("label", cat);
                return map;
            })
            .collect(Collectors.toList());
    }
}
