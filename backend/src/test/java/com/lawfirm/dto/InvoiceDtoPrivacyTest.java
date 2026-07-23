package com.lawfirm.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceDtoPrivacyTest {

    @Test
    void serializedInvoiceExposesAvailabilityButNeverServerPath() throws Exception {
        InvoiceDTO invoice = new InvoiceDTO();
        invoice.setId(18L);
        invoice.setTitle("测试客户");
        invoice.setFeedbackFileAvailable(true);

        String json = new ObjectMapper().writeValueAsString(invoice);

        assertTrue(json.contains("\"feedbackFileAvailable\":true"));
        assertFalse(json.contains("invoiceFilePath"));
        assertFalse(json.contains("/Volumes/"));
    }
}
