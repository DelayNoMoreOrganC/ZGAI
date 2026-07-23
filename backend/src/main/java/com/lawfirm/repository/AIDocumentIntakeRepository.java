package com.lawfirm.repository;

import com.lawfirm.entity.AIDocumentIntake;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AIDocumentIntakeRepository extends JpaRepository<AIDocumentIntake, Long> {
    List<AIDocumentIntake> findTop200ByExpiresAtBeforeAndStatusInAndDeletedFalseOrderByExpiresAtAsc(
            LocalDateTime expiresAt, Collection<String> statuses);
}
