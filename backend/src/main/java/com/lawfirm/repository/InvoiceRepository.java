package com.lawfirm.repository;

import com.lawfirm.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 开票记录Repository
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * 根据案件ID查找开票记录
     */
    List<Invoice> findByCaseId(Long caseId);

    /**
     * 根据发票号码查找
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * 根据发票状态查找
     */
    List<Invoice> findByStatus(String status);

    /**
     * 根据开票日期范围查找
     */
    List<Invoice> findByBillingDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 统计案件开票金额
     */
    @Query("SELECT SUM(i.amount) FROM Invoice i WHERE i.caseId = :caseId")
    Double calculateTotalInvoiceAmount(@Param("caseId") Long caseId);

    /**
     * 检查发票号是否存在
     */
    boolean existsByInvoiceNumber(String invoiceNumber);
}
