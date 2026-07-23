package com.lawfirm.service;

import com.lawfirm.dto.InvoiceDTO;
import com.lawfirm.entity.Invoice;
import com.lawfirm.entity.Todo;
import com.lawfirm.entity.User;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.InvoiceRepository;
import com.lawfirm.repository.TodoRepository;
import com.lawfirm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.DataBinder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvoiceServiceTest {

    @TempDir
    Path tempDir;

    private InvoiceRepository invoiceRepository;
    private UserRepository userRepository;
    private TodoRepository todoRepository;
    private UserPermissionService userPermissionService;
    private InvoiceService service;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        userRepository = mock(UserRepository.class);
        todoRepository = mock(TodoRepository.class);
        userPermissionService = mock(UserPermissionService.class);
        service = new InvoiceService(
                invoiceRepository,
                mock(CaseRepository.class),
                userRepository,
                todoRepository,
                userPermissionService);
    }

    @Test
    void ordinaryLawyerPageIsRestrictedToOwnApplications() {
        User lawyer = user(7L, "律师甲", "律师");
        Invoice ownInvoice = invoice(11L, 7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(lawyer));
        when(invoiceRepository.findByApplicantId(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(ownInvoice)));

        var result = service.getInvoices(1, 20, 7L);

        assertEquals(1L, result.getTotal());
        assertEquals(7L, result.getRecords().get(0).getApplicantId());
        verify(invoiceRepository).findByApplicantId(any(Long.class), any(Pageable.class));
    }

    @Test
    void financeUserCanViewAllApplications() {
        User finance = user(8L, "财务甲", "财务管理");
        when(userRepository.findById(8L)).thenReturn(Optional.of(finance));
        when(userPermissionService.hasPermission(finance, "FINANCE_VIEW")).thenReturn(true);
        when(invoiceRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(invoice(12L, 7L))));

        var result = service.getInvoices(1, 20, 8L);

        assertEquals(1L, result.getTotal());
        verify(invoiceRepository).findAll(any(Pageable.class));
    }

    @Test
    void invoiceAssignmentPrefersProcessorThenFinanceRole() {
        User finance = user(8L, "财务甲", "财务管理");
        InvoiceDTO request = new InvoiceDTO();
        request.setInvoiceType("增值税普通发票");
        request.setAmount(new BigDecimal("1000"));
        request.setTitle("测试客户");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "律师甲", "律师")));
        when(userPermissionService.findFirstActiveUserByPermission(
                "INVOICE_PROCESS", java.util.Arrays.asList("INVOICE_PROCESSOR", "FINANCE")))
                .thenReturn(Optional.of(finance));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice saved = invocation.getArgument(0);
            saved.setId(16L);
            return saved;
        });

        InvoiceDTO created = service.createInvoice(request, 7L);

        assertEquals(8L, created.getCashierId());
        verify(userPermissionService).findFirstActiveUserByPermission(
                "INVOICE_PROCESS", java.util.Arrays.asList("INVOICE_PROCESSOR", "FINANCE"));
    }

    @Test
    void unrelatedLawyerCannotReadInvoiceDetail() {
        User lawyer = user(9L, "律师乙", "律师");
        when(userRepository.findById(9L)).thenReturn(Optional.of(lawyer));
        when(invoiceRepository.findById(13L)).thenReturn(Optional.of(invoice(13L, 7L)));

        assertThrows(AccessDeniedException.class, () -> service.getInvoiceById(13L, 9L));
    }

    @Test
    void directorCanReadAnyInvoiceDetail() {
        User director = user(10L, "主任甲", "主任");
        when(userRepository.findById(10L)).thenReturn(Optional.of(director));
        when(invoiceRepository.findById(14L)).thenReturn(Optional.of(invoice(14L, 7L)));
        when(userPermissionService.hasPermission(director, "FINANCE_VIEW")).thenReturn(true);

        assertEquals(14L, service.getInvoiceById(14L, 10L).getId());
    }

    @Test
    void multipartDateUsesIsoFormat() {
        InvoiceDTO dto = new InvoiceDTO();
        DataBinder binder = new DataBinder(dto);
        binder.setConversionService(new DefaultFormattingConversionService());

        binder.bind(new MutablePropertyValues(Collections.singletonMap("billingDate", "2026-07-24")));

        assertEquals(LocalDate.of(2026, 7, 24), dto.getBillingDate());
    }

    @Test
    void ordinaryLawyerCannotIssueInvoice() {
        User lawyer = user(9L, "律师乙", "律师");
        when(userRepository.findById(9L)).thenReturn(Optional.of(lawyer));
        when(invoiceRepository.findById(13L)).thenReturn(Optional.of(invoice(13L, 9L)));

        assertThrows(AccessDeniedException.class,
                () -> service.issueInvoice(13L, new InvoiceDTO(), null, 9L));
    }

    @Test
    void financeFeedbackIsStoredUnderConfiguredAbsoluteUploadRoot() throws Exception {
        User finance = user(8L, "财务甲", "财务管理");
        Invoice pending = invoice(15L, 7L);
        InvoiceDTO dto = new InvoiceDTO();
        dto.setInvoiceNumber("INV-2026-001");
        dto.setBillingDate(LocalDate.of(2026, 7, 24));
        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf", "%PDF-test".getBytes());
        when(userRepository.findById(8L)).thenReturn(Optional.of(finance));
        when(invoiceRepository.findById(15L)).thenReturn(Optional.of(pending));
        when(userPermissionService.hasPermission(finance, "INVOICE_PROCESS")).thenReturn(true);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "uploadPath", tempDir.toString());

        var result = service.issueInvoice(15L, dto, file, 8L);

        assertEquals("FEEDBACK_UPLOADED", result.getStatus());
        assertTrue(result.isFeedbackFileAvailable());
        Path stored = Path.of(pending.getInvoiceFilePath());
        assertEquals(tempDir.toAbsolutePath(), stored.getParent().getParent().getParent());
        assertEquals("%PDF-test", Files.readString(stored));
        ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).save(todoCaptor.capture());
        assertTrue(todoCaptor.getValue().getDescription().contains("进入财务管理下载"));
        assertFalse(todoCaptor.getValue().getDescription().contains(tempDir.toString()));
    }

    @Test
    void financeFeedbackRejectsUnsupportedExecutableFile() {
        User finance = user(8L, "财务甲", "财务管理");
        Invoice pending = invoice(17L, 7L);
        InvoiceDTO dto = new InvoiceDTO();
        dto.setInvoiceNumber("INV-2026-002");
        dto.setBillingDate(LocalDate.of(2026, 7, 24));
        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.exe", "application/octet-stream", "binary".getBytes());
        when(userRepository.findById(8L)).thenReturn(Optional.of(finance));
        when(invoiceRepository.findById(17L)).thenReturn(Optional.of(pending));
        when(userPermissionService.hasPermission(finance, "INVOICE_PROCESS")).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.issueInvoice(17L, dto, file, 8L));

        assertEquals("电子发票反馈文件仅支持PDF、OFD、JPG和PNG格式", error.getMessage());
    }

    private User user(Long id, String username, String position) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(username);
        user.setPosition(position);
        return user;
    }

    private Invoice invoice(Long id, Long applicantId) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setApplicantId(applicantId);
        invoice.setInvoiceType("ELECTRONIC_NORMAL");
        invoice.setTitle("测试客户");
        invoice.setAmount(BigDecimal.TEN);
        invoice.setStatus("PENDING");
        return invoice;
    }
}
