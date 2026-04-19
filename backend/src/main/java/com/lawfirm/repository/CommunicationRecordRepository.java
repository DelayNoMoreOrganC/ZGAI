package com.lawfirm.repository;

import com.lawfirm.entity.CommunicationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 沟通记录Repository
 */
@Repository
public interface CommunicationRecordRepository extends JpaRepository<CommunicationRecord, Long> {

    /**
     * 根据客户ID查找沟通记录
     */
    List<CommunicationRecord> findByClientIdOrderByCommunicationDateDesc(Long clientId);
}
