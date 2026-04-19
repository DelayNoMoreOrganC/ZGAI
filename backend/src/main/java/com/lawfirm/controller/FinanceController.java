package com.lawfirm.controller;

import com.lawfirm.dto.FinanceRecordDTO;
import com.lawfirm.dto.InvoiceDTO;
import com.lawfirm.dto.PaymentDTO;
import com.lawfirm.service.FinanceRecordService;
import com.lawfirm.service.InvoiceService;
import com.lawfirm.service.PaymentService;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 财务管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceRecordService financeRecordService;
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;
    private final SecurityUtils securityUtils;

    // ==================== 财务记录相关接口 ====================

    /**
     * 创建财务记录
     * POST /api/finance/expenses
     */
    @PostMapping("/expenses")
    public Result<FinanceRecordDTO> createFinanceRecord(@RequestBody FinanceRecordDTO dto) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            FinanceRecordDTO result = financeRecordService.createFinanceRecord(dto, userId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("创建财务记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建财务记录异常", e);
            return Result.error("创建财务记录失败");
        }
    }

    /**
     * 更新财务记录
     * PUT /api/finance/expenses/{id}
     */
    @PutMapping("/expenses/{id}")
    public Result<FinanceRecordDTO> updateFinanceRecord(@PathVariable Long id, @RequestBody FinanceRecordDTO dto) {
        try {
            FinanceRecordDTO result = financeRecordService.updateFinanceRecord(id, dto);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("更新财务记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新财务记录异常", e);
            return Result.error("更新财务记录失败");
        }
    }

    /**
     * 删除财务记录
     * DELETE /api/finance/expenses/{id}
     */
    @DeleteMapping("/expenses/{id}")
    public Result<Void> deleteFinanceRecord(@PathVariable Long id) {
        try {
            financeRecordService.deleteFinanceRecord(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.error("删除财务记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除财务记录异常", e);
            return Result.error("删除财务记录失败");
        }
    }

    /**
     * 查询财务记录详情
     * GET /api/finance/expenses/{id}
     */
    @GetMapping("/expenses/{id}")
    public Result<FinanceRecordDTO> getFinanceRecord(@PathVariable Long id) {
        try {
            FinanceRecordDTO result = financeRecordService.getFinanceRecordById(id);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("查询财务记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("查询财务记录异常", e);
            return Result.error("查询财务记录失败");
        }
    }

    /**
     * 查询案件的财务记录
     * GET /api/finance/expenses/case/{caseId}
     */
    @GetMapping("/expenses/case/{caseId}")
    public Result<List<FinanceRecordDTO>> getFinanceRecordsByCase(@PathVariable Long caseId) {
        try {
            List<FinanceRecordDTO> result = financeRecordService.getFinanceRecordsByCase(caseId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询案件财务记录异常", e);
            return Result.error("查询案件财务记录失败");
        }
    }

    /**
     * 按日期范围查询财务记录
     * GET /api/finance/expenses/range?start={start}&end={end}
     */
    @GetMapping("/expenses/range")
    public Result<List<FinanceRecordDTO>> getFinanceRecordsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        try {
            List<FinanceRecordDTO> result = financeRecordService.getFinanceRecordsByDateRange(start, end);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询财务记录异常", e);
            return Result.error("查询财务记录失败");
        }
    }

    /**
     * 分页查询财务记录
     * GET /api/finance/expenses?page={page}&size={size}
     */
    @GetMapping("/expenses")
    public Result<com.lawfirm.util.PageResult<FinanceRecordDTO>> getFinanceRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            var result = financeRecordService.getFinanceRecords(page, size);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询财务记录异常", e);
            return Result.error("分页查询财务记录失败");
        }
    }

    // ==================== 收款记录相关接口 ====================

    /**
     * 创建收款记录
     * POST /api/finance/payments
     */
    @PostMapping("/payments")
    public Result<PaymentDTO> createPayment(@RequestBody PaymentDTO dto) {
        try {
            PaymentDTO result = paymentService.createPayment(dto);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("创建收款记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建收款记录异常", e);
            return Result.error("创建收款记录失败");
        }
    }

    /**
     * 更新收款记录
     * PUT /api/finance/payments/{id}
     */
    @PutMapping("/payments/{id}")
    public Result<PaymentDTO> updatePayment(@PathVariable Long id, @RequestBody PaymentDTO dto) {
        try {
            PaymentDTO result = paymentService.updatePayment(id, dto);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("更新收款记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新收款记录异常", e);
            return Result.error("更新收款记录失败");
        }
    }

    /**
     * 删除收款记录
     * DELETE /api/finance/payments/{id}
     */
    @DeleteMapping("/payments/{id}")
    public Result<Void> deletePayment(@PathVariable Long id) {
        try {
            paymentService.deletePayment(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.error("删除收款记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除收款记录异常", e);
            return Result.error("删除收款记录失败");
        }
    }

    /**
     * 查询收款记录详情
     * GET /api/finance/payments/{id}
     */
    @GetMapping("/payments/{id}")
    public Result<PaymentDTO> getPayment(@PathVariable Long id) {
        try {
            PaymentDTO result = paymentService.getPaymentById(id);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("查询收款记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("查询收款记录异常", e);
            return Result.error("查询收款记录失败");
        }
    }

    /**
     * 查询案件的收款记录
     * GET /api/finance/payments/case/{caseId}
     */
    @GetMapping("/payments/case/{caseId}")
    public Result<List<PaymentDTO>> getPaymentsByCase(@PathVariable Long caseId) {
        try {
            List<PaymentDTO> result = paymentService.getPaymentsByCase(caseId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询案件收款记录异常", e);
            return Result.error("查询案件收款记录失败");
        }
    }

    /**
     * 分页查询收款记录
     * GET /api/finance/payments?page={page}&size={size}
     */
    @GetMapping("/payments")
    public Result<com.lawfirm.util.PageResult<PaymentDTO>> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            var result = paymentService.getPayments(page, size);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询收款记录异常", e);
            return Result.error("分页查询收款记录失败");
        }
    }

    // ==================== 开票记录相关接口 ====================

    /**
     * 创建开票记录
     * POST /api/finance/invoices
     */
    @PostMapping("/invoices")
    public Result<InvoiceDTO> createInvoice(@RequestBody InvoiceDTO dto) {
        try {
            InvoiceDTO result = invoiceService.createInvoice(dto);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("创建开票记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建开票记录异常", e);
            return Result.error("创建开票记录失败");
        }
    }

    /**
     * 更新开票记录
     * PUT /api/finance/invoices/{id}
     */
    @PutMapping("/invoices/{id}")
    public Result<InvoiceDTO> updateInvoice(@PathVariable Long id, @RequestBody InvoiceDTO dto) {
        try {
            InvoiceDTO result = invoiceService.updateInvoice(id, dto);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("更新开票记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新开票记录异常", e);
            return Result.error("更新开票记录失败");
        }
    }

    /**
     * 删除开票记录
     * DELETE /api/finance/invoices/{id}
     */
    @DeleteMapping("/invoices/{id}")
    public Result<Void> deleteInvoice(@PathVariable Long id) {
        try {
            invoiceService.deleteInvoice(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.error("删除开票记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除开票记录异常", e);
            return Result.error("删除开票记录失败");
        }
    }

    /**
     * 查询开票记录详情
     * GET /api/finance/invoices/{id}
     */
    @GetMapping("/invoices/{id}")
    public Result<InvoiceDTO> getInvoice(@PathVariable Long id) {
        try {
            InvoiceDTO result = invoiceService.getInvoiceById(id);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("查询开票记录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("查询开票记录异常", e);
            return Result.error("查询开票记录失败");
        }
    }

    /**
     * 查询案件的开票记录
     * GET /api/finance/invoices/case/{caseId}
     */
    @GetMapping("/invoices/case/{caseId}")
    public Result<List<InvoiceDTO>> getInvoicesByCase(@PathVariable Long caseId) {
        try {
            List<InvoiceDTO> result = invoiceService.getInvoicesByCase(caseId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询案件开票记录异常", e);
            return Result.error("查询案件开票记录失败");
        }
    }

    /**
     * 按状态查询开票记录
     * GET /api/finance/invoices/status/{status}
     */
    @GetMapping("/invoices/status/{status}")
    public Result<List<InvoiceDTO>> getInvoicesByStatus(@PathVariable String status) {
        try {
            List<InvoiceDTO> result = invoiceService.getInvoicesByStatus(status);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询开票记录异常", e);
            return Result.error("查询开票记录失败");
        }
    }

    /**
     * 分页查询开票记录
     * GET /api/finance/invoices?page={page}&size={size}
     */
    @GetMapping("/invoices")
    public Result<com.lawfirm.util.PageResult<InvoiceDTO>> getInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            var result = invoiceService.getInvoices(page, size);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询开票记录异常", e);
            return Result.error("分页查询开票记录失败");
        }
    }

    // ==================== 律师费管理相关接口 ====================

    /**
     * 查询律师费（已收/待收）
     * GET /api/finance/fees
     */
    @GetMapping("/fees")
    public Result<java.util.Map<String, Object>> getLawyerFees(
            @RequestParam(required = false) Long caseId) {
        try {
            java.util.Map<String, Object> result = new java.util.HashMap<>();

            // 已收律师费（从收款记录中统计）
            List<PaymentDTO> receivedPayments;
            if (caseId != null) {
                receivedPayments = paymentService.getPaymentsByCase(caseId);
            } else {
                // 查询所有收款记录（最近100条）
                com.lawfirm.util.PageResult<PaymentDTO> allPayments = paymentService.getPayments(0, 100);
                receivedPayments = allPayments.getRecords();
            }

            // 统计已收律师费总额
            double totalReceived = receivedPayments.stream()
                    .filter(p -> p.getPaymentAmount() != null)
                    .mapToDouble(p -> p.getPaymentAmount().doubleValue())
                    .sum();

            result.put("receivedFees", totalReceived);
            result.put("pendingFees", 0);
            result.put("totalFees", totalReceived);
            result.put("receivedPayments", receivedPayments);

            return Result.success(result);
        } catch (Exception e) {
            log.error("查询律师费失败", e);
            return Result.error("查询律师费失败: " + e.getMessage());
        }
    }

    /**
     * 查询案件律师费明细
     * GET /api/finance/fees/case/{caseId}
     */
    @GetMapping("/fees/case/{caseId}")
    public Result<java.util.Map<String, Object>> getCaseLawyerFees(@PathVariable Long caseId) {
        try {
            java.util.Map<String, Object> result = new java.util.HashMap<>();

            // 获取该案件的收款记录
            List<PaymentDTO> payments = paymentService.getPaymentsByCase(caseId);

            double receivedAmount = payments.stream()
                    .filter(p -> p.getPaymentAmount() != null)
                    .mapToDouble(p -> p.getPaymentAmount().doubleValue())
                    .sum();

            result.put("caseId", caseId);
            result.put("receivedAmount", receivedAmount);
            result.put("paymentRecords", payments);

            return Result.success(result);
        } catch (Exception e) {
            log.error("查询案件律师费失败", e);
            return Result.error("查询案件律师费失败: " + e.getMessage());
        }
    }

    // ==================== 财务统计相关接口 ====================

    /**
     * 财务概览数据
     * GET /api/finance/dashboard
     */
    @GetMapping("/dashboard")
    public Result<java.util.Map<String, Object>> getFinanceDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            java.util.Map<String, Object> dashboard = financeRecordService.getFinanceDashboard(startDate, endDate);
            return Result.success(dashboard);
        } catch (Exception e) {
            log.error("获取财务概览失败", e);
            return Result.error("获取财务概览失败");
        }
    }

    /**
     * 案件财务汇总
     * GET /api/finance/summary/{caseId}
     */
    @GetMapping("/summary/{caseId}")
    public Result<java.util.Map<String, Object>> getCaseFinanceSummary(@PathVariable Long caseId) {
        try {
            java.util.Map<String, Object> summary = financeRecordService.getCaseFinanceSummary(caseId);
            return Result.success(summary);
        } catch (Exception e) {
            log.error("获取案件财务汇总失败", e);
            return Result.error("获取案件财务汇总失败");
        }
    }
}
