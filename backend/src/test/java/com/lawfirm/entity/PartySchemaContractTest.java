package com.lawfirm.entity;

import com.lawfirm.enums.PartyRole;
import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PartySchemaContractTest {

    @Test
    void partyRoleColumnFitsEverySupportedRoleCode() throws Exception {
        Column column = Party.class.getDeclaredField("partyRole").getAnnotation(Column.class);
        int longestRoleCode = Arrays.stream(PartyRole.values())
                .map(Enum::name)
                .mapToInt(String::length)
                .max()
                .orElseThrow();

        assertTrue(column.length() >= longestRoleCode,
                "party_role column must fit every PartyRole enum code");
    }
}
