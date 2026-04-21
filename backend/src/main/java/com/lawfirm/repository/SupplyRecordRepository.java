package com.lawfirm.repository;

import com.lawfirm.entity.SupplyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 出入库记录Repository
 */
@Repository
public interface SupplyRecordRepository extends JpaRepository<SupplyRecord, Long> {

    /**
     * 根据用品ID查找记录
     */
    List<SupplyRecord> findBySupplyIdOrderByCreatedAtDesc(Long supplyId);
}
