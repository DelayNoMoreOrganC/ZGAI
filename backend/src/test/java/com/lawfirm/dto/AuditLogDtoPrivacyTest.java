package com.lawfirm.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogDtoPrivacyTest {

    @Test
    void administrativeAuditViewDoesNotExposeParamsOrInternalErrors() throws Exception {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(1L);
        dto.setOperation("重置用户密码");
        dto.setStatus(1);

        String json = new ObjectMapper().writeValueAsString(dto);

        assertTrue(json.contains("重置用户密码"));
        assertFalse(json.contains("params"));
        assertFalse(json.contains("errorMsg"));
        assertFalse(json.contains("password"));
    }
}
