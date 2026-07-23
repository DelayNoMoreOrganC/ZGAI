package com.lawfirm.repository;

import com.lawfirm.entity.ConflictCheckRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConflictCheckRecordRepository extends JpaRepository<ConflictCheckRecord, Long> {

    List<ConflictCheckRecord> findBySubjectNameOrderByCreatedAtDesc(String subjectName);

    List<ConflictCheckRecord> findByCheckedByOrderByCreatedAtDesc(Long checkedBy);

    List<ConflictCheckRecord> findByCaseIdOrderByCreatedAtAsc(Long caseId);
}
