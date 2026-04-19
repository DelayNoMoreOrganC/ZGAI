package com.lawfirm.service;

import com.lawfirm.dto.FinanceRecordDTO;
import com.lawfirm.entity.FinanceRecord;
import com.lawfirm.entity.Payment;
import com.lawfirm.repository.FinanceRecordRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 财务记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceRecordService {

    private final FinanceRecordRepository financeRecordRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    /**
     * 创建财务记录
     */
    @Transactional
    public FinanceRecordDTO createFinanceRecord(FinanceRecordDTO dto, Long userId) {
        // 验证案件是否存在
        if (dto.getCaseId() != null && !caseRepository.existsById(dto.getCaseId())) {
            throw new IllegalArgumentException("案件不存在");
        }

        FinanceRecord record = new FinanceRecord();
        record.setCaseId(dto.getCaseId());
        record.setFinanceType(dto.getFinanceType());
        record.setAmount(dto.getAmount());
        record.setExpenseType(dto.getExpenseType());
        record.setPaymentMethod(dto.getPaymentMethod());
        record.setTransactionDate(dto.getTransactionDate());
        record.setDescription(dto.getDescription());
        record.setOperatorId(userId);
        record.setReceipt(dto.getReceipt());

        record = financeRecordRepository.save(record);
        log.info("创建财务记录成功: {}", record.getId());

        return convertToDTO(record);
    }

    /**
     * 更新财务记录
     */
    @Transactional
    public FinanceRecordDTO updateFinanceRecord(Long id, FinanceRecordDTO dto) {
        FinanceRecord record = financeRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("财务记录不存在"));

        // 验证案件是否存在
        if (dto.getCaseId() != null && !caseRepository.existsById(dto.getCaseId())) {
            throw new IllegalArgumentException("案件不存在");
        }

        record.setCaseId(dto.getCaseId());
        record.setFinanceType(dto.getFinanceType());
        record.setAmount(dto.getAmount());
        record.setExpenseType(dto.getExpenseType());
        record.setPaymentMethod(dto.getPaymentMethod());
        record.setTransactionDate(dto.getTransactionDate());
        record.setDescription(dto.getDescription());
        record.setReceipt(dto.getReceipt());

        record = financeRecordRepository.save(record);
        log.info("更新财务记录成功: {}", id);

        return convertToDTO(record);
    }

    /**
     * 删除财务记录
     */
    @Transactional
    public void deleteFinanceRecord(Long id) {
        if (!financeRecordRepository.existsById(id)) {
            throw new IllegalArgumentException("财务记录不存在");
        }
        financeRecordRepository.deleteById(id);
        log.info("删除财务记录成功: {}", id);
    }

    /**
     * 根据ID查询财务记录
     */
    public FinanceRecordDTO getFinanceRecordById(Long id) {
        return financeRecordRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new IllegalArgumentException("财务记录不存在"));
    }

    /**
     * 查询案件的财务记录
     */
    public List<FinanceRecordDTO> getFinanceRecordsByCase(Long caseId) {
        return financeRecordRepository.findByCaseId(caseId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按类型查询财务记录
     */
    public List<FinanceRecordDTO> getFinanceRecordsByType(String financeType) {
        return financeRecordRepository.findByFinanceType(financeType).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按日期范围查询财务记录
     */
    public List<FinanceRecordDTO> getFinanceRecordsByDateRange(LocalDate startDate, LocalDate endDate) {
        return financeRecordRepository.findByTransactionDateBetween(startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询财务记录
     */
    public com.lawfirm.util.PageResult<FinanceRecordDTO> getFinanceRecords(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));

        // 使用 Spring Data 分页查询，避免全表加载
        org.springframework.data.domain.Page<FinanceRecord> recordPage = financeRecordRepository.findAll(pageable);

        List<FinanceRecordDTO> dtoList = recordPage.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new com.lawfirm.util.PageResult<>((long) page, (long) size, recordPage.getTotalElements(), dtoList);
    }

    /**
     * 转换为DTO
     */
    private FinanceRecordDTO convertToDTO(FinanceRecord record) {
        FinanceRecordDTO dto = new FinanceRecordDTO();
        dto.setId(record.getId());
        dto.setCaseId(record.getCaseId());
        dto.setFinanceType(record.getFinanceType());
        dto.setAmount(record.getAmount());
        dto.setExpenseType(record.getExpenseType());
        dto.setPaymentMethod(record.getPaymentMethod());
        dto.setTransactionDate(record.getTransactionDate());
        dto.setDescription(record.getDescription());
        dto.setOperatorId(record.getOperatorId());
        dto.setReceipt(record.getReceipt());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());

        // 加载案件名称
        if (record.getCaseId() != null) {
            caseRepository.findById(record.getCaseId()).ifPresent(c -> dto.setCaseName(c.getCaseName()));
        }

        // 加载操作人名称
        userRepository.findById(record.getOperatorId()).ifPresent(u -> dto.setOperatorName(u.getRealName()));

        return dto;
    }

    /**
     * 获取财务概览数据（Dashboard）
     */
    public java.util.Map<String, Object> getFinanceDashboard(LocalDate startDate, LocalDate endDate) {
        // 默认查询本月数据
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        java.util.Map<String, Object> dashboard = new java.util.HashMap<>();

        // 收入统计
        List<FinanceRecord> incomeRecords = financeRecordRepository.findByFinanceTypeAndTransactionDateBetween(
                "INCOME", startDate, endDate);
        BigDecimal totalIncome = incomeRecords.stream()
                .map(FinanceRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 支出统计
        List<FinanceRecord> expenseRecords = financeRecordRepository.findByFinanceTypeAndTransactionDateBetween(
                "EXPENSE", startDate, endDate);
        BigDecimal totalExpense = expenseRecords.stream()
                .map(FinanceRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 净收入
        BigDecimal netIncome = totalIncome.subtract(totalExpense);

        // 待收款（预测收入）- 使用数据库查询优化
        BigDecimal pendingIncome = caseRepository.findByAttorneyFeeIsNotNull().stream()
                .map(c -> {
                    // 计算案件待收律师费
                    BigDecimal totalFee = c.getAttorneyFee();
                    BigDecimal received = java.util.Optional.ofNullable(
                            financeRecordRepository.findByCaseId(c.getId()).stream()
                                    .filter(r -> "INCOME".equals(r.getFinanceType()))
                                    .map(FinanceRecord::getAmount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    ).orElse(BigDecimal.ZERO);
                    return totalFee.subtract(received);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dashboard.put("startDate", startDate);
        dashboard.put("endDate", endDate);
        dashboard.put("totalIncome", totalIncome);
        dashboard.put("totalExpense", totalExpense);
        dashboard.put("netIncome", netIncome);
        dashboard.put("pendingIncome", pendingIncome);
        dashboard.put("incomeCount", incomeRecords.size());
        dashboard.put("expenseCount", expenseRecords.size());

        log.info("财务概览数据查询完成: {} - {}", startDate, endDate);
        return dashboard;
    }

    /**
     * 获取案件财务汇总
     */
    public java.util.Map<String, Object> getCaseFinanceSummary(Long caseId) {
        java.util.Map<String, Object> summary = new java.util.HashMap<>();

        // 查询案件信息
        var caseOpt = caseRepository.findById(caseId);
        if (caseOpt.isEmpty()) {
            throw new IllegalArgumentException("案件不存在");
        }
        var caseEntity = caseOpt.get();

        // 案件基本信息
        summary.put("caseId", caseId);
        summary.put("caseName", caseEntity.getCaseName());
        summary.put("attorneyFee", caseEntity.getAttorneyFee());
        summary.put("actualReceived", caseEntity.getActualReceived());

        // 收入记录（FinanceRecord表）
        List<FinanceRecord> incomeRecords = financeRecordRepository.findByCaseIdAndFinanceType(caseId, "INCOME");
        BigDecimal incomeFromRecords = incomeRecords.stream()
                .map(FinanceRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 收款记录（Payment表）
        List<Payment> paymentRecords = paymentRepository.findByCaseId(caseId);
        BigDecimal incomeFromPayments = paymentRecords.stream()
                .map(Payment::getPaymentAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 总收入 = FinanceRecord收入 + Payment收款
        BigDecimal totalIncome = incomeFromRecords.add(incomeFromPayments);

        // 支出记录
        List<FinanceRecord> expenseRecords = financeRecordRepository.findByCaseIdAndFinanceType(caseId, "EXPENSE");
        BigDecimal totalExpense = expenseRecords.stream()
                .map(FinanceRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 净收益
        BigDecimal netProfit = totalIncome.subtract(totalExpense);

        // 收款进度
        BigDecimal paymentProgress = BigDecimal.ZERO;
        if (caseEntity.getAttorneyFee() != null && caseEntity.getAttorneyFee().compareTo(BigDecimal.ZERO) > 0) {
            paymentProgress = totalIncome.divide(caseEntity.getAttorneyFee(), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        summary.put("netProfit", netProfit);
        summary.put("incomeRecords", incomeRecords.size());
        summary.put("expenseRecords", expenseRecords.size());
        summary.put("incomeFromRecords", incomeFromRecords);
        summary.put("incomeFromPayments", incomeFromPayments);
        summary.put("paymentCount", paymentRecords.size());
        summary.put("paymentProgress", paymentProgress);

        log.info("案件财务汇总查询完成: caseId={}, totalIncome={}, fromRecords={}, fromPayments={}",
            caseId, totalIncome, incomeFromRecords, incomeFromPayments);
        return summary;
    }
}
