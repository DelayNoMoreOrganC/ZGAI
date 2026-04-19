package com.lawfirm.service;

import com.lawfirm.repository.FinanceRecordRepository;
import com.lawfirm.repository.PaymentRepository;
import com.lawfirm.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 财务汇总服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceSummaryService {

    private final FinanceRecordRepository financeRecordRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * 获取案件财务汇总
     */
    public Map<String, Object> getCaseFinanceSummary(Long caseId) {
        Map<String, Object> summary = new HashMap<>();

        // 收入统计
        Double totalIncome = financeRecordRepository.calculateTotalIncome(caseId);
        summary.put("totalIncome", totalIncome != null ? totalIncome : 0.0);

        // 支出统计
        Double totalExpense = financeRecordRepository.calculateTotalExpense(caseId);
        summary.put("totalExpense", totalExpense != null ? totalExpense : 0.0);

        // 收款统计
        Double totalPayment = paymentRepository.calculateTotalPayment(caseId);
        summary.put("totalPayment", totalPayment != null ? totalPayment : 0.0);

        // 开票统计
        Double totalInvoice = invoiceRepository.calculateTotalInvoiceAmount(caseId);
        summary.put("totalInvoice", totalInvoice != null ? totalInvoice : 0.0);

        // 利润
        BigDecimal profit = BigDecimal.valueOf(totalIncome != null ? totalIncome : 0.0)
                .subtract(BigDecimal.valueOf(totalExpense != null ? totalExpense : 0.0));
        summary.put("profit", profit);

        // 应收账款（开票未收款）
        BigDecimal accountsReceivable = BigDecimal.valueOf(totalInvoice != null ? totalInvoice : 0.0)
                .subtract(BigDecimal.valueOf(totalPayment != null ? totalPayment : 0.0));
        summary.put("accountsReceivable", accountsReceivable);

        return summary;
    }

    /**
     * 获取月度财务汇总
     */
    public Map<String, Object> getMonthlyFinanceSummary(int year, int month) {
        Map<String, Object> summary = new HashMap<>();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // 月度收入
        var monthlyRecords = financeRecordRepository.findByTransactionDateBetween(startDate, endDate);
        Double monthlyIncome = monthlyRecords.stream()
                .filter(r -> "INCOME".equals(r.getFinanceType()))
                .mapToDouble(r -> r.getAmount().doubleValue())
                .sum();
        summary.put("monthlyIncome", monthlyIncome);

        // 月度支出
        Double monthlyExpense = monthlyRecords.stream()
                .filter(r -> "EXPENSE".equals(r.getFinanceType()))
                .mapToDouble(r -> r.getAmount().doubleValue())
                .sum();
        summary.put("monthlyExpense", monthlyExpense);

        // 月度收款
        var monthlyPayments = paymentRepository.findByPaymentDateBetween(startDate, endDate);
        Double monthlyPayment = monthlyPayments.stream()
                .mapToDouble(p -> p.getPaymentAmount().doubleValue())
                .sum();
        summary.put("monthlyPayment", monthlyPayment);

        // 月度开票
        var monthlyInvoices = invoiceRepository.findByBillingDateBetween(startDate, endDate);
        Double monthlyInvoice = monthlyInvoices.stream()
                .mapToDouble(i -> i.getAmount().doubleValue())
                .sum();
        summary.put("monthlyInvoice", monthlyInvoice);

        return summary;
    }

    /**
     * 获取年度财务汇总
     */
    public Map<String, Object> getYearlyFinanceSummary(int year) {
        Map<String, Object> summary = new HashMap<>();

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // 年度收入
        var yearlyRecords = financeRecordRepository.findByTransactionDateBetween(startDate, endDate);
        Double yearlyIncome = yearlyRecords.stream()
                .filter(r -> "INCOME".equals(r.getFinanceType()))
                .mapToDouble(r -> r.getAmount().doubleValue())
                .sum();
        summary.put("yearlyIncome", yearlyIncome);

        // 年度支出
        Double yearlyExpense = yearlyRecords.stream()
                .filter(r -> "EXPENSE".equals(r.getFinanceType()))
                .mapToDouble(r -> r.getAmount().doubleValue())
                .sum();
        summary.put("yearlyExpense", yearlyExpense);

        // 年度收款
        var yearlyPayments = paymentRepository.findByPaymentDateBetween(startDate, endDate);
        Double yearlyPayment = yearlyPayments.stream()
                .mapToDouble(p -> p.getPaymentAmount().doubleValue())
                .sum();
        summary.put("yearlyPayment", yearlyPayment);

        // 年度开票
        var yearlyInvoices = invoiceRepository.findByBillingDateBetween(startDate, endDate);
        Double yearlyInvoice = yearlyInvoices.stream()
                .mapToDouble(i -> i.getAmount().doubleValue())
                .sum();
        summary.put("yearlyInvoice", yearlyInvoice);

        // 年度利润
        BigDecimal yearlyProfit = BigDecimal.valueOf(yearlyIncome)
                .subtract(BigDecimal.valueOf(yearlyExpense));
        summary.put("yearlyProfit", yearlyProfit);

        return summary;
    }

    /**
     * 获取全局财务统计
     */
    public Map<String, Object> getGlobalFinanceStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // 总收入
        var allRecords = financeRecordRepository.findAll();
        Double totalIncome = allRecords.stream()
                .filter(r -> "INCOME".equals(r.getFinanceType()))
                .mapToDouble(r -> r.getAmount().doubleValue())
                .sum();
        statistics.put("totalIncome", totalIncome);

        // 总支出
        Double totalExpense = allRecords.stream()
                .filter(r -> "EXPENSE".equals(r.getFinanceType()))
                .mapToDouble(r -> r.getAmount().doubleValue())
                .sum();
        statistics.put("totalExpense", totalExpense);

        // 总收款
        var allPayments = paymentRepository.findAll();
        Double totalPayment = allPayments.stream()
                .mapToDouble(p -> p.getPaymentAmount().doubleValue())
                .sum();
        statistics.put("totalPayment", totalPayment);

        // 总开票
        var allInvoices = invoiceRepository.findAll();
        Double totalInvoice = allInvoices.stream()
                .mapToDouble(i -> i.getAmount().doubleValue())
                .sum();
        statistics.put("totalInvoice", totalInvoice);

        // 总利润
        BigDecimal totalProfit = BigDecimal.valueOf(totalIncome)
                .subtract(BigDecimal.valueOf(totalExpense));
        statistics.put("totalProfit", totalProfit);

        // 应收账款
        BigDecimal accountsReceivable = BigDecimal.valueOf(totalInvoice)
                .subtract(BigDecimal.valueOf(totalPayment));
        statistics.put("accountsReceivable", accountsReceivable);

        return statistics;
    }
}
