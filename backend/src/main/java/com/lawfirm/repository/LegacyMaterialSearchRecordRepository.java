package com.lawfirm.repository;

import com.lawfirm.entity.LegacyMaterialSearchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 旧系统资料检索记录。
 */
@Repository
public interface LegacyMaterialSearchRecordRepository extends JpaRepository<LegacyMaterialSearchRecord, Long> {
}
