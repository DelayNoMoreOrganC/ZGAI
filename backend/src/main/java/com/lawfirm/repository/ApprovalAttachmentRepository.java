package com.lawfirm.repository;

import com.lawfirm.entity.ApprovalAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalAttachmentRepository extends JpaRepository<ApprovalAttachment, Long> {
    List<ApprovalAttachment> findByApprovalIdAndDeletedFalseOrderByIdAsc(Long approvalId);
    Optional<ApprovalAttachment> findByIdAndApprovalIdAndDeletedFalse(Long id, Long approvalId);
}
