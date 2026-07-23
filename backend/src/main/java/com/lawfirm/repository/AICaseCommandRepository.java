package com.lawfirm.repository;

import com.lawfirm.entity.AICaseCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AICaseCommandRepository extends JpaRepository<AICaseCommand, Long> {
    Optional<AICaseCommand> findByUserIdAndIdempotencyKeyAndDeletedFalse(Long userId, String idempotencyKey);

    long countByPrivacySanitizedAtIsNull();

    List<AICaseCommand> findAllByPrivacySanitizedAtIsNullOrderByIdAsc();

    Optional<AICaseCommand> findFirstByPrivacySanitizedAtIsNullOrderByCreatedAtDesc();
}
