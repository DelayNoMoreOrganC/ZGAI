package com.lawfirm.service;

import com.lawfirm.dto.InvoiceDTO;
import com.lawfirm.entity.Invoice;
import com.lawfirm.repository.InvoiceRepository;
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
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 开票记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CaseRepository caseRepository;

    /**
     * 创建开票记录
     */
    @Transactional
    public InvoiceDTO createInvoice(InvoiceDTO dto) {
        // 验证案件是否存在
        if (dto.getCaseId() != null && !caseRepository.existsById(dto.getCaseId())) {
            throw new IllegalArgumentException("案件不存在");
        }

        // 检查发票号是否已存在
        if (dto.getInvoiceNumber() != null && invoiceRepository.existsByInvoiceNumber(dto.getInvoiceNumber())) {
            throw new IllegalArgumentException("发票号已存在");
        }

        Invoice invoice = new Invoice();
        invoice.setCaseId(dto.getCaseId());
        invoice.setInvoiceNumber(dto.getInvoiceNumber());
        invoice.setInvoiceType(dto.getInvoiceType());
        invoice.setAmount(dto.getAmount());
        invoice.setTitle(dto.getTitle());
        invoice.setTaxNumber(dto.getTaxNumber());
        invoice.setBillingDate(dto.getBillingDate());
        invoice.setStatus(dto.getStatus() != null ? dto.getStatus() : "PENDING");

        invoice = invoiceRepository.save(invoice);
        log.info("创建开票记录成功: {}", invoice.getId());

        return convertToDTO(invoice);
    }

    /**
     * 更新开票记录
     */
    @Transactional
    public InvoiceDTO updateInvoice(Long id, InvoiceDTO dto) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("开票记录不存在"));

        // 验证案件是否存在
        if (dto.getCaseId() != null && !caseRepository.existsById(dto.getCaseId())) {
            throw new IllegalArgumentException("案件不存在");
        }

        // 检查发票号是否与其他记录冲突
        if (dto.getInvoiceNumber() != null && !dto.getInvoiceNumber().equals(invoice.getInvoiceNumber())) {
            Optional<Invoice> existing = invoiceRepository.findByInvoiceNumber(dto.getInvoiceNumber());
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new IllegalArgumentException("发票号已被使用");
            }
        }

        invoice.setCaseId(dto.getCaseId());
        invoice.setInvoiceNumber(dto.getInvoiceNumber());
        invoice.setInvoiceType(dto.getInvoiceType());
        invoice.setAmount(dto.getAmount());
        invoice.setTitle(dto.getTitle());
        invoice.setTaxNumber(dto.getTaxNumber());
        invoice.setBillingDate(dto.getBillingDate());
        invoice.setStatus(dto.getStatus());

        invoice = invoiceRepository.save(invoice);
        log.info("更新开票记录成功: {}", id);

        return convertToDTO(invoice);
    }

    /**
     * 删除开票记录
     */
    @Transactional
    public void deleteInvoice(Long id) {
        if (!invoiceRepository.existsById(id)) {
            throw new IllegalArgumentException("开票记录不存在");
        }
        invoiceRepository.deleteById(id);
        log.info("删除开票记录成功: {}", id);
    }

    /**
     * 根据ID查询开票记录
     */
    public InvoiceDTO getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new IllegalArgumentException("开票记录不存在"));
    }

    /**
     * 查询案件的开票记录
     */
    public List<InvoiceDTO> getInvoicesByCase(Long caseId) {
        return invoiceRepository.findByCaseId(caseId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按状态查询
     */
    public List<InvoiceDTO> getInvoicesByStatus(String status) {
        return invoiceRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按日期范围查询
     */
    public List<InvoiceDTO> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate) {
        return invoiceRepository.findByBillingDateBetween(startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询开票记录
     */
    public com.lawfirm.util.PageResult<InvoiceDTO> getInvoices(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "billingDate"));

        // 使用 Spring Data 分页查询，避免全表加载
        org.springframework.data.domain.Page<Invoice> invoicePage = invoiceRepository.findAll(pageable);

        List<InvoiceDTO> dtoList = invoicePage.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new com.lawfirm.util.PageResult<>((long) page, (long) size, invoicePage.getTotalElements(), dtoList);
    }

    /**
     * 转换为DTO
     */
    private InvoiceDTO convertToDTO(Invoice invoice) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(invoice.getId());
        dto.setCaseId(invoice.getCaseId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setInvoiceType(invoice.getInvoiceType());
        dto.setAmount(invoice.getAmount());
        dto.setTitle(invoice.getTitle());
        dto.setTaxNumber(invoice.getTaxNumber());
        dto.setBillingDate(invoice.getBillingDate());
        dto.setStatus(invoice.getStatus());
        dto.setCreatedAt(invoice.getCreatedAt());
        dto.setUpdatedAt(invoice.getUpdatedAt());

        // 加载案件名称
        if (invoice.getCaseId() != null) {
            caseRepository.findById(invoice.getCaseId()).ifPresent(c -> dto.setCaseName(c.getCaseName()));
        }

        return dto;
    }
}
