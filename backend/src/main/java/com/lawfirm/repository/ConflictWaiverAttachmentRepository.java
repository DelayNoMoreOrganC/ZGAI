package com.lawfirm.repository;

import com.lawfirm.entity.ConflictWaiverAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConflictWaiverAttachmentRepository extends JpaRepository<ConflictWaiverAttachment, Long> {

    List<ConflictWaiverAttachment> findByConflictCheckRecordIdOrderByCreatedAtAsc(Long conflictCheckRecordId);

    boolean existsByConflictCheckRecordIdAndContentSha256(Long conflictCheckRecordId, String contentSha256);
}
