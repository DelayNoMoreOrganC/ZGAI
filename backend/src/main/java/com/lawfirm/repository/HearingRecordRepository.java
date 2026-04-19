package com.lawfirm.repository;

import com.lawfirm.entity.HearingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HearingRecordRepository extends JpaRepository<HearingRecord, Long> {
    List<HearingRecord> findByCaseIdAndDeletedFalseOrderByHearingDateDesc(Long caseId);
}
