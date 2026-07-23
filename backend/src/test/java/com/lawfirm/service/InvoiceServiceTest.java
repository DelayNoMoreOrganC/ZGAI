package com.lawfirm.service;

import com.lawfirm.entity.Invoice;
import com.lawfirm.entity.User;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.InvoiceRepository;
import com.lawfirm.repository.TodoRepository;
import com.lawfirm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvoiceServiceTest {

    private InvoiceRepository invoiceRepository;
    private UserRepository userRepository;
    private UserPermissionService userPermissionService;
    private InvoiceService service;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        userRepository = mock(UserRepository.class);
        userPermissionService = mock(UserPermissionService.class);
        service = new InvoiceService(
                invoiceRepository,
                mock(CaseRepository.class),
                userRepository,
                mock(TodoRepository.class),
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
    void unrelatedLawyerCannotReadInvoiceDetail() {
        User lawyer = user(9L, "律师乙", "律师");
        when(userRepository.findById(9L)).thenReturn(Optional.of(lawyer));
        when(invoiceRepository.findById(13L)).thenReturn(Optional.of(invoice(13L, 7L)));

        assertThrows(IllegalArgumentException.class, () -> service.getInvoiceById(13L, 9L));
    }

    @Test
    void directorCanReadAnyInvoiceDetail() {
        User director = user(10L, "主任甲", "主任");
        when(userRepository.findById(10L)).thenReturn(Optional.of(director));
        when(invoiceRepository.findById(14L)).thenReturn(Optional.of(invoice(14L, 7L)));
        when(userPermissionService.hasPermission(director, "FINANCE_VIEW")).thenReturn(true);

        assertEquals(14L, service.getInvoiceById(14L, 10L).getId());
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
