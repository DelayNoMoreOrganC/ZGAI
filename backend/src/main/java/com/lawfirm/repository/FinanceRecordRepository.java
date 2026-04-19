package com.lawfirm.repository;

import com.lawfirm.entity.FinanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 财务记录Repository
 */
@Repository
public interface FinanceRecordRepository extends JpaRepository<FinanceRecord, Long> {

    /**
     * 根据案件ID查找财务记录
     */
    List<FinanceRecord> findByCaseId(Long caseId);

    /**
     * 根据财务类型查找记录
     */
    List<FinanceRecord> findByFinanceType(String financeType);

    /**
     * 根据交易日期范围查找记录
     */
    List<FinanceRecord> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 统计案件总收入
     */
    @Query("SELECT SUM(f.amount) FROM FinanceRecord f WHERE f.caseId = :caseId AND f.financeType = 'INCOME'")
    Double calculateTotalIncome(@Param("caseId") Long caseId);

    /**
     * 统计案件总支出
     */
    @Query("SELECT SUM(f.amount) FROM FinanceRecord f WHERE f.caseId = :caseId AND f.financeType = 'EXPENSE'")
    Double calculateTotalExpense(@Param("caseId") Long caseId);

    /**
     * 根据财务类型和日期范围查找记录
     */
    List<FinanceRecord> findByFinanceTypeAndTransactionDateBetween(String financeType, LocalDate startDate, LocalDate endDate);

    /**
     * 根据案件ID和财务类型查找记录
     */
    List<FinanceRecord> findByCaseIdAndFinanceType(Long caseId, String financeType);
}
