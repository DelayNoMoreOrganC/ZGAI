package com.lawfirm.repository;

import com.lawfirm.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 收款记录Repository
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 根据案件ID查找收款记录
     */
    List<Payment> findByCaseId(Long caseId);

    /**
     * 根据收款方式查找
     */
    List<Payment> findByPaymentMethod(String paymentMethod);

    /**
     * 根据收款日期范围查找
     */
    List<Payment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 根据付款人查找
     */
    List<Payment> findByPayer(String payer);

    /**
     * 统计案件收款总额
     */
    @Query("SELECT SUM(p.paymentAmount) FROM Payment p WHERE p.caseId = :caseId")
    Double calculateTotalPayment(@Param("caseId") Long caseId);

    /**
     * 根据交易流水号查找
     */
    List<Payment> findByTransactionNumber(String transactionNumber);

    /**
     * 统计所有收款记录数量（分页优化）
     */
    long count();
}
