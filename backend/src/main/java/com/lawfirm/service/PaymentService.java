package com.lawfirm.service;

import com.lawfirm.dto.PaymentDTO;
import com.lawfirm.entity.Payment;
import com.lawfirm.repository.PaymentRepository;
import com.lawfirm.repository.CaseRepository;
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
 * 收款记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CaseRepository caseRepository;

    /**
     * 创建收款记录
     */
    @Transactional
    public PaymentDTO createPayment(PaymentDTO dto) {
        // 验证案件是否存在
        if (dto.getCaseId() != null && !caseRepository.existsById(dto.getCaseId())) {
            throw new IllegalArgumentException("案件不存在");
        }

        Payment payment = new Payment();
        payment.setCaseId(dto.getCaseId());
        payment.setPaymentAmount(dto.getPaymentAmount());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setPayer(dto.getPayer());
        payment.setBankAccount(dto.getBankAccount());
        payment.setTransactionNumber(dto.getTransactionNumber());
        payment.setNotes(dto.getNotes());

        payment = paymentRepository.save(payment);
        log.info("创建收款记录成功: {}", payment.getId());

        return convertToDTO(payment);
    }

    /**
     * 更新收款记录
     */
    @Transactional
    public PaymentDTO updatePayment(Long id, PaymentDTO dto) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("收款记录不存在"));

        // 验证案件是否存在
        if (dto.getCaseId() != null && !caseRepository.existsById(dto.getCaseId())) {
            throw new IllegalArgumentException("案件不存在");
        }

        payment.setCaseId(dto.getCaseId());
        payment.setPaymentAmount(dto.getPaymentAmount());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setPayer(dto.getPayer());
        payment.setBankAccount(dto.getBankAccount());
        payment.setTransactionNumber(dto.getTransactionNumber());
        payment.setNotes(dto.getNotes());

        payment = paymentRepository.save(payment);
        log.info("更新收款记录成功: {}", id);

        return convertToDTO(payment);
    }

    /**
     * 删除收款记录
     */
    @Transactional
    public void deletePayment(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new IllegalArgumentException("收款记录不存在");
        }
        paymentRepository.deleteById(id);
        log.info("删除收款记录成功: {}", id);
    }

    /**
     * 根据ID查询收款记录
     */
    public PaymentDTO getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new IllegalArgumentException("收款记录不存在"));
    }

    /**
     * 查询案件的收款记录
     */
    public List<PaymentDTO> getPaymentsByCase(Long caseId) {
        return paymentRepository.findByCaseId(caseId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按收款方式查询
     */
    public List<PaymentDTO> getPaymentsByMethod(String paymentMethod) {
        return paymentRepository.findByPaymentMethod(paymentMethod).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按日期范围查询
     */
    public List<PaymentDTO> getPaymentsByDateRange(LocalDate startDate, LocalDate endDate) {
        return paymentRepository.findByPaymentDateBetween(startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询收款记录
     */
    public com.lawfirm.util.PageResult<PaymentDTO> getPayments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paymentDate"));

        // 使用 Spring Data 分页查询，避免全表加载
        org.springframework.data.domain.Page<Payment> paymentPage = paymentRepository.findAll(pageable);

        List<PaymentDTO> dtoList = paymentPage.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new com.lawfirm.util.PageResult<>((long) page, (long) size, paymentPage.getTotalElements(), dtoList);
    }

    /**
     * 转换为DTO
     */
    private PaymentDTO convertToDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setCaseId(payment.getCaseId());
        dto.setPaymentAmount(payment.getPaymentAmount());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPayer(payment.getPayer());
        dto.setBankAccount(payment.getBankAccount());
        dto.setTransactionNumber(payment.getTransactionNumber());
        dto.setNotes(payment.getNotes());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());

        // 加载案件名称
        if (payment.getCaseId() != null) {
            caseRepository.findById(payment.getCaseId()).ifPresent(c -> dto.setCaseName(c.getCaseName()));
        }

        return dto;
    }
}
