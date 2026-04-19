package com.lawfirm.service;

import com.lawfirm.dto.AnnouncementCreateRequest;
import com.lawfirm.dto.AnnouncementDTO;
import com.lawfirm.entity.Announcement;
import com.lawfirm.entity.AnnouncementRead;
import com.lawfirm.repository.AnnouncementReadRepository;
import com.lawfirm.repository.AnnouncementRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公告服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final UserRepository userRepository;

    /**
     * 目标范围常量
     */
    public static final String SCOPE_ALL = "ALL";  // 全员
    public static final String SCOPE_DEPARTMENT = "DEPARTMENT";  // 指定部门
    public static final String SCOPE_ROLE = "ROLE";  // 指定角色

    /**
     * 创建公告
     */
    @Transactional
    public AnnouncementDTO createAnnouncement(AnnouncementCreateRequest request, Long publisherId) {
        Announcement announcement = new Announcement();
        BeanUtils.copyProperties(request, announcement);
        announcement.setPublisherId(publisherId);
        announcement.setPublishDate(LocalDateTime.now());

        announcement = announcementRepository.save(announcement);

        return toDTO(announcement, null);
    }

    /**
     * 更新公告
     */
    @Transactional
    public AnnouncementDTO updateAnnouncement(Long announcementId, AnnouncementCreateRequest request) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("公告不存在"));

        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setTargetScope(request.getTargetScope());
        announcement.setPriority(request.getPriority());
        announcement.setAttachments(request.getAttachments());

        announcement = announcementRepository.save(announcement);

        return toDTO(announcement, null);
    }

    /**
     * 删除公告
     */
    @Transactional
    public void deleteAnnouncement(Long announcementId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("公告不存在"));

        announcement.setDeleted(true);
        announcementRepository.save(announcement);

        // 删除相关的阅读记录
        List<AnnouncementRead> reads = announcementReadRepository.findByAnnouncementIdOrderByReadTimeDesc(announcementId);
        announcementReadRepository.deleteAll(reads);
    }

    /**
     * 获取公告列表
     */
    public Page<AnnouncementDTO> getAnnouncementList(int page, int size, Long currentUserId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishDate"));

        Page<Announcement> announcementPage = announcementRepository.findAll(pageable);

        // 获取用户已读公告ID列表
        List<Long> readAnnouncementIds = announcementReadRepository.findByUserIdOrderByReadTimeDesc(currentUserId)
                .stream()
                .map(AnnouncementRead::getAnnouncementId)
                .collect(Collectors.toList());

        // 统计每个公告的阅读数
        Map<Long, Long> readCounts = announcementPage.getContent()
                .stream()
                .collect(Collectors.toMap(
                        Announcement::getId,
                        announcement -> announcementReadRepository.countByAnnouncementId(announcement.getId())
                ));

        return announcementPage.map(announcement -> {
            AnnouncementDTO dto = toDTO(announcement, currentUserId);
            Long readCount = readCounts.get(announcement.getId());
            dto.setReadCount(readCount != null ? readCount.intValue() : 0);
            dto.setIsRead(readAnnouncementIds.contains(announcement.getId()));
            return dto;
        });
    }

    /**
     * 获取公告详情
     */
    @Transactional
    public AnnouncementDTO getAnnouncementDetail(Long announcementId, Long currentUserId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("公告不存在"));

        // 标记为已读
        markAsRead(announcementId, currentUserId);

        int readCount = announcementReadRepository.countByAnnouncementId(announcementId).intValue();
        AnnouncementDTO dto = toDTO(announcement, currentUserId);
        dto.setReadCount(readCount);
        dto.setIsRead(true);

        return dto;
    }

    /**
     * 标记为已读
     */
    @Transactional
    public void markAsRead(Long announcementId, Long userId) {
        if (announcementReadRepository.findByAnnouncementIdAndUserId(announcementId, userId).isPresent()) {
            return;  // 已阅读过
        }

        AnnouncementRead read = new AnnouncementRead();
        read.setAnnouncementId(announcementId);
        read.setUserId(userId);
        read.setReadTime(LocalDateTime.now());
        announcementReadRepository.save(read);
    }

    /**
     * 获取未读公告数量
     */
    public long getUnreadCount(Long userId) {
        // 使用数据库查询优化，直接查询未删除的公告
        List<Announcement> activeAnnouncements = announcementRepository.findByDeletedFalseOrderByPublishDateDesc();

        List<Long> readAnnouncementIds = announcementReadRepository.findByUserIdOrderByReadTimeDesc(userId)
                .stream()
                .map(AnnouncementRead::getAnnouncementId)
                .collect(Collectors.toList());

        return activeAnnouncements.stream()
                .filter(announcement -> !readAnnouncementIds.contains(announcement.getId()))
                .count();
    }

    /**
     * 获取目标范围列表
     */
    public List<Map<String, String>> getTargetScopes() {
        return List.of(
                Map.of("code", SCOPE_ALL, "name", "全员"),
                Map.of("code", SCOPE_DEPARTMENT, "name", "指定部门"),
                Map.of("code", SCOPE_ROLE, "name", "指定角色")
        );
    }

    // 辅助方法

    private AnnouncementDTO toDTO(Announcement announcement, Long currentUserId) {
        AnnouncementDTO dto = new AnnouncementDTO();
        BeanUtils.copyProperties(announcement, dto);

        dto.setPublisherName(getUserName(announcement.getPublisherId()));
        dto.setTargetScopeDesc(getScopeDesc(announcement.getTargetScope()));

        // 检查是否已读
        if (currentUserId != null) {
            boolean isRead = announcementReadRepository
                    .findByAnnouncementIdAndUserId(announcement.getId(), currentUserId)
                    .isPresent();
            dto.setIsRead(isRead);
        }

        return dto;
    }

    private String getUserName(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRealName())
                .orElse("未知");
    }

    private String getScopeDesc(String scope) {
        if (scope == null) return null;
        switch (scope) {
            case SCOPE_ALL: return "全员";
            case SCOPE_DEPARTMENT: return "指定部门";
            case SCOPE_ROLE: return "指定角色";
            default: return scope;
        }
    }
}
