package com.lawfirm.repository;

import com.lawfirm.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告Repository
 */
@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /**
     * 根据发布人查找公告列表
     */
    List<Announcement> findByPublisherIdOrderByPublishDateDesc(Long publisherId);

    /**
     * 根据目标范围查找公告列表
     */
    List<Announcement> findByTargetScopeOrderByPublishDateDesc(String targetScope);

    /**
     * 查找指定日期之后发布的公告
     */
    List<Announcement> findByPublishDateAfterOrderByPublishDateDesc(LocalDateTime publishDate);

    /**
     * 根据优先级查找公告列表
     */
    List<Announcement> findByPriorityOrderByPublishDateDesc(Integer priority);

    /**
     * 查找所有未删除的公告（性能优化）
     */
    List<Announcement> findByDeletedFalseOrderByPublishDateDesc();
}
