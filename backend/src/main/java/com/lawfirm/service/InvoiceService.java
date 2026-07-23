package com.lawfirm.service;

import com.lawfirm.dto.InvoiceDTO;
import com.lawfirm.entity.Invoice;
import com.lawfirm.entity.Todo;
import com.lawfirm.entity.User;
import com.lawfirm.repository.InvoiceRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.TodoRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final UserRepository userRepository;
    private final TodoRepository todoRepository;
    private final UserPermissionService userPermissionService;

    @Value("${file.upload-path:./uploads/}")
    private String uploadPath;

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_FEEDBACK_UPLOADED = "FEEDBACK_UPLOADED";
    private static final String STATUS_COMPLETED = "COMPLETED";

    /**
     * 创建开票记录
     */
    @Transactional
    public InvoiceDTO createInvoice(InvoiceDTO dto, Long applicantId) {
        // 验证案件是否存在
        if (dto.getCaseId() != null && !caseRepository.existsById(dto.getCaseId())) {
            throw new IllegalArgumentException("案件不存在");
        }

        // 检查发票号是否已存在
        if (StringUtils.hasText(dto.getInvoiceNumber()) && invoiceRepository.existsByInvoiceNumber(dto.getInvoiceNumber())) {
            throw new IllegalArgumentException("发票号已存在");
        }

        User cashier = findCashier();
        Invoice invoice = new Invoice();
        applyInvoiceFields(invoice, dto);
        if (invoice.getBillingDate() == null) {
            invoice.setBillingDate(LocalDate.now());
        }
        invoice.setApplicantId(applicantId);
        invoice.setCashierId(cashier.getId());
        invoice.setStatus(STATUS_PENDING);

        invoice = invoiceRepository.save(invoice);
        createCashierTodo(invoice, applicantId, cashier.getId());
        log.info("创建开票申请成功: {}", invoice.getId());

        return convertToDTO(invoice);
    }

    /**
     * 更新开票记录
     */
    @Transactional
    public InvoiceDTO updateInvoice(Long id, InvoiceDTO dto, Long currentUserId) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("开票记录不存在"));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
        boolean applicant = invoice.getApplicantId() != null && invoice.getApplicantId().equals(currentUserId);
        boolean financeUser = canProcessInvoices(currentUser);
        if (!applicant && !financeUser) {
            throw new AccessDeniedException("无权修改该开票申请");
        }
        if (STATUS_COMPLETED.equals(invoice.getStatus())) {
            throw new IllegalArgumentException("开票已完成，申请记录已锁定");
        }
        if (applicant && !STATUS_PENDING.equals(invoice.getStatus())) {
            throw new IllegalArgumentException("财务已反馈开票文件，申请内容不可修改");
        }

        // 验证案件是否存在
        if (dto.getCaseId() != null && !caseRepository.existsById(dto.getCaseId())) {
            throw new IllegalArgumentException("案件不存在");
        }

        // 检查发票号是否与其他记录冲突
        if (StringUtils.hasText(dto.getInvoiceNumber()) && !dto.getInvoiceNumber().equals(invoice.getInvoiceNumber())) {
            Optional<Invoice> existing = invoiceRepository.findByInvoiceNumber(dto.getInvoiceNumber());
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new IllegalArgumentException("发票号已被使用");
            }
        }

        applyInvoiceFields(invoice, dto);
        if (invoice.getBillingDate() == null) {
            invoice.setBillingDate(LocalDate.now());
        }
        if (financeUser && StringUtils.hasText(dto.getStatus())) {
            invoice.setStatus(dto.getStatus());
        }

        invoice = invoiceRepository.save(invoice);
        log.info("更新开票记录成功: {}", id);

        return convertToDTO(invoice);
    }

    public Path getInvoiceFilePath(Long id, Long currentUserId) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("开票申请不存在"));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
        boolean applicant = invoice.getApplicantId() != null && invoice.getApplicantId().equals(currentUserId);
        boolean financeUser = canViewAllInvoices(currentUser);
        if (!applicant && !financeUser) {
            throw new AccessDeniedException("无权查看该反馈文件");
        }
        if (!StringUtils.hasText(invoice.getInvoiceFilePath())) {
            throw new IllegalArgumentException("暂无反馈文件");
        }
        Path file = Paths.get(invoice.getInvoiceFilePath()).normalize();
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("反馈文件不存在");
        }
        return file;
    }

    /**
     * 财务人员上传电子发票反馈文件。
     */
    @Transactional
    public InvoiceDTO issueInvoice(Long id, InvoiceDTO dto, MultipartFile file, Long currentUserId) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("开票申请不存在"));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
        if (!canProcessInvoices(currentUser)) {
            throw new AccessDeniedException("仅财务管理人员可处理开票申请");
        }
        if (STATUS_COMPLETED.equals(invoice.getStatus())) {
            throw new IllegalArgumentException("开票已完成，申请记录已锁定");
        }

        if (!StringUtils.hasText(dto.getInvoiceNumber())) {
            throw new IllegalArgumentException("请填写发票号码");
        }
        if (dto.getBillingDate() == null) {
            throw new IllegalArgumentException("请选择开票日期");
        }
        if (!dto.getInvoiceNumber().equals(invoice.getInvoiceNumber())) {
            invoiceRepository.findByInvoiceNumber(dto.getInvoiceNumber())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("发票号已被使用");
                    });
        }

        invoice.setInvoiceNumber(dto.getInvoiceNumber());
        invoice.setBillingDate(dto.getBillingDate());
        if (StringUtils.hasText(dto.getInvoiceType())) {
            invoice.setInvoiceType(dto.getInvoiceType());
        }
        if (dto.getAmount() != null) {
            invoice.setAmount(dto.getAmount());
        }
        if (StringUtils.hasText(dto.getTitle())) {
            invoice.setTitle(dto.getTitle());
        }
        if (StringUtils.hasText(dto.getRemark())) {
            invoice.setRemark(dto.getRemark());
        }
        if (file != null && !file.isEmpty()) {
            invoice.setInvoiceFilePath(saveInvoiceFile(invoice.getId(), file));
        }
        if (!StringUtils.hasText(invoice.getInvoiceFilePath())) {
            throw new IllegalArgumentException("请上传电子发票反馈文件");
        }
        invoice.setStatus(STATUS_FEEDBACK_UPLOADED);

        invoice = invoiceRepository.save(invoice);
        createApplicantFeedbackTodo(invoice);
        log.info("发票开具反馈完成: {}", id);
        return convertToDTO(invoice);
    }

    /**
     * 财务人员确认开票完成并锁定申请记录。
     */
    @Transactional
    public InvoiceDTO completeInvoice(Long id, Long currentUserId) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("开票申请不存在"));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
        if (!canProcessInvoices(currentUser)) {
            throw new AccessDeniedException("仅财务管理人员可确认完成开票");
        }
        if (STATUS_COMPLETED.equals(invoice.getStatus())) {
            return convertToDTO(invoice);
        }
        if (!StringUtils.hasText(invoice.getInvoiceFilePath())) {
            throw new IllegalArgumentException("尚未上传电子发票反馈文件，不能完成开票");
        }
        invoice.setStatus(STATUS_COMPLETED);
        invoice = invoiceRepository.save(invoice);
        return convertToDTO(invoice);
    }

    /**
     * 删除开票记录
     */
    @Transactional
    public void deleteInvoice(Long id, Long currentUserId) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("开票申请不存在"));
        if (!STATUS_PENDING.equals(invoice.getStatus())) {
            throw new IllegalArgumentException("仅待审查阶段的开票申请可删除");
        }
        if (StringUtils.hasText(invoice.getInvoiceFilePath())) {
            throw new IllegalArgumentException("已反馈发票文件的申请不可删除");
        }
        if (invoice.getApplicantId() == null || !invoice.getApplicantId().equals(currentUserId)) {
            throw new IllegalArgumentException("仅申请发起人可删除待审查申请");
        }
        invoiceRepository.deleteById(id);
        log.info("删除开票记录成功: {}", id);
    }

    /**
     * 根据ID查询开票记录
     */
    public InvoiceDTO getInvoiceById(Long id, Long currentUserId) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("开票记录不存在"));
        assertInvoiceVisible(invoice, currentUserId);
        return convertToDTO(invoice);
    }

    /**
     * 查询案件的开票记录
     */
    public List<InvoiceDTO> getInvoicesByCase(Long caseId, Long currentUserId) {
        User currentUser = requireCurrentUser(currentUserId);
        List<Invoice> invoices = canViewAllInvoices(currentUser)
                ? invoiceRepository.findByCaseId(caseId)
                : invoiceRepository.findByCaseIdAndApplicantId(caseId, currentUserId);
        return invoices.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按状态查询
     */
    public List<InvoiceDTO> getInvoicesByStatus(String status, Long currentUserId) {
        User currentUser = requireCurrentUser(currentUserId);
        List<Invoice> invoices = canViewAllInvoices(currentUser)
                ? invoiceRepository.findByStatus(status)
                : invoiceRepository.findByStatusAndApplicantId(status, currentUserId);
        return invoices.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按日期范围查询
     */
    public List<InvoiceDTO> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate, Long currentUserId) {
        User currentUser = requireCurrentUser(currentUserId);
        List<Invoice> invoices = canViewAllInvoices(currentUser)
                ? invoiceRepository.findByBillingDateBetween(startDate, endDate)
                : invoiceRepository.findByBillingDateBetweenAndApplicantId(startDate, endDate, currentUserId);
        return invoices.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询开票记录
     */
    public com.lawfirm.util.PageResult<InvoiceDTO> getInvoices(int page, int size, Long currentUserId) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "billingDate"));

        User currentUser = requireCurrentUser(currentUserId);
        org.springframework.data.domain.Page<Invoice> invoicePage = canViewAllInvoices(currentUser)
                ? invoiceRepository.findAll(pageable)
                : invoiceRepository.findByApplicantId(currentUserId, pageable);

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
        dto.setContractNo(invoice.getContractNo());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setInvoiceType(invoice.getInvoiceType());
        dto.setAmount(invoice.getAmount());
        dto.setTitle(invoice.getTitle());
        dto.setTaxNumber(invoice.getTaxNumber());
        dto.setBillingDate(invoice.getBillingDate());
        dto.setExecutionDepartment(invoice.getExecutionDepartment());
        dto.setSourceUserName(invoice.getSourceUserName());
        dto.setInvoiceContent(invoice.getInvoiceContent());
        dto.setRemark(invoice.getRemark());
        dto.setAddressPhone(invoice.getAddressPhone());
        dto.setBankAccount(invoice.getBankAccount());
        dto.setApplicantId(invoice.getApplicantId());
        dto.setCashierId(invoice.getCashierId());
        dto.setInvoiceFilePath(invoice.getInvoiceFilePath());
        dto.setStatus(invoice.getStatus());
        dto.setCreatedAt(invoice.getCreatedAt());
        dto.setUpdatedAt(invoice.getUpdatedAt());

        // 加载案件名称
        if (invoice.getCaseId() != null) {
            caseRepository.findById(invoice.getCaseId()).ifPresent(c -> dto.setCaseName(c.getCaseName()));
        }

        return dto;
    }

    private void applyInvoiceFields(Invoice invoice, InvoiceDTO dto) {
        invoice.setCaseId(dto.getCaseId());
        invoice.setContractNo(dto.getContractNo());
        if (StringUtils.hasText(dto.getInvoiceNumber())) {
            invoice.setInvoiceNumber(dto.getInvoiceNumber());
        }
        invoice.setInvoiceType(dto.getInvoiceType());
        invoice.setAmount(dto.getAmount());
        invoice.setTitle(dto.getTitle());
        invoice.setTaxNumber(dto.getTaxNumber());
        if (dto.getBillingDate() != null) {
            invoice.setBillingDate(dto.getBillingDate());
        }
        invoice.setExecutionDepartment(dto.getExecutionDepartment());
        invoice.setSourceUserName(dto.getSourceUserName());
        invoice.setInvoiceContent(dto.getInvoiceContent());
        invoice.setRemark(dto.getRemark());
        invoice.setAddressPhone(dto.getAddressPhone());
        invoice.setBankAccount(dto.getBankAccount());
    }

    private User findCashier() {
        return userPermissionService.findFirstActiveUserByPermission("INVOICE_PROCESS", "INVOICE_PROCESSOR")
                .orElseThrow(() -> new IllegalArgumentException("未找到具有开票处理权限的账号，请先配置财务角色"));
    }

    private boolean canProcessInvoices(User user) {
        return userPermissionService.hasPermission(user, "INVOICE_PROCESS");
    }

    private boolean canViewAllInvoices(User user) {
        return userPermissionService.hasPermission(user, "FINANCE_VIEW");
    }

    private User requireCurrentUser(Long currentUserId) {
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
    }

    private void assertInvoiceVisible(Invoice invoice, Long currentUserId) {
        User currentUser = requireCurrentUser(currentUserId);
        if (canViewAllInvoices(currentUser) || currentUserId.equals(invoice.getApplicantId())) {
            return;
        }
        throw new AccessDeniedException("无权查看其他人员的开票申请");
    }

    private void createCashierTodo(Invoice invoice, Long applicantId, Long cashierId) {
        Todo todo = new Todo();
        todo.setTitle("开票申请审批：" + invoice.getTitle());
        todo.setDescription(buildCashierTodoDescription(invoice, applicantId));
        todo.setStatus(STATUS_PENDING);
        todo.setPriority("HIGH");
        todo.setDueDate(LocalDateTime.now().plusDays(3));
        todo.setAssigneeId(cashierId);
        todo.setCaseId(invoice.getCaseId());
        todo.setReminder(true);
        todoRepository.save(todo);
    }

    private void createApplicantFeedbackTodo(Invoice invoice) {
        if (invoice.getApplicantId() == null) {
            return;
        }
        Todo todo = new Todo();
        todo.setTitle("电子发票已开具：" + invoice.getTitle());
        todo.setDescription("发票号码：" + invoice.getInvoiceNumber()
                + "；开票日期：" + invoice.getBillingDate()
                + "；电子发票文件：" + (StringUtils.hasText(invoice.getInvoiceFilePath()) ? invoice.getInvoiceFilePath() : "未上传"));
        todo.setStatus(STATUS_PENDING);
        todo.setPriority("NORMAL");
        todo.setAssigneeId(invoice.getApplicantId());
        todo.setCaseId(invoice.getCaseId());
        todoRepository.save(todo);
    }

    private String buildCashierTodoDescription(Invoice invoice, Long applicantId) {
        return "申请人ID：" + applicantId
                + "；合同号：" + nullToBlank(invoice.getContractNo())
                + "；发票种类：" + nullToBlank(invoice.getInvoiceType())
                + "；金额：" + invoice.getAmount()
                + "；执行部门：" + nullToBlank(invoice.getExecutionDepartment())
                + "；案源人：" + nullToBlank(invoice.getSourceUserName())
                + "；发票内容：" + nullToBlank(invoice.getInvoiceContent());
    }

    private String saveInvoiceFile(Long invoiceId, MultipartFile file) {
        try {
            String filename = System.currentTimeMillis() + "_" + StringUtils.cleanPath(file.getOriginalFilename());
            Path directory = Paths.get(uploadPath)
                    .toAbsolutePath()
                    .normalize()
                    .resolve("invoices")
                    .resolve(String.valueOf(invoiceId));
            Files.createDirectories(directory);
            Path target = directory.resolve(filename).normalize();
            if (!target.startsWith(directory)) {
                throw new IllegalArgumentException("电子发票文件名无效");
            }
            file.transferTo(target.toFile());
            return target.toString();
        } catch (IOException e) {
            throw new IllegalArgumentException("电子发票上传失败");
        }
    }

    private String nullToBlank(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
