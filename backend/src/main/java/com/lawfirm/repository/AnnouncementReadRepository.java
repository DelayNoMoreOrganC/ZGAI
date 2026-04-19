package com.lawfirm.repository;

import com.lawfirm.entity.AnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 公告阅读记录Repository
 */
@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {

    /**
     * 根据公告ID查找所有阅读记录
     */
    List<AnnouncementRead> findByAnnouncementIdOrderByReadTimeDesc(Long announcementId);

    /**
     * 根据用户ID查找所有阅读记录
     */
    List<AnnouncementRead> findByUserIdOrderByReadTimeDesc(Long userId);

    /**
     * 查找用户是否已阅读某公告
     */
    Optional<AnnouncementRead> findByAnnouncementIdAndUserId(Long announcementId, Long userId);

    /**
     * 统计某公告的阅读数量
     */
    Long countByAnnouncementId(Long announcementId);
}
