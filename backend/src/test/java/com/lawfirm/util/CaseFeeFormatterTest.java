package com.lawfirm.util;

import com.lawfirm.entity.Case;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaseFeeFormatterTest {

    @Test
    void formatsFixedFee() {
        Case caseEntity = caseWithMethod("FIXED");
        caseEntity.setAttorneyFee(new BigDecimal("10000.00"));

        assertEquals("固定收费 10000元", CaseFeeFormatter.format(caseEntity));
    }

    @Test
    void formatsContingentRatio() {
        Case caseEntity = caseWithMethod("CONTINGENT");
        caseEntity.setRiskRatio(new BigDecimal("5.00"));

        assertEquals("风险收费 5%", CaseFeeFormatter.format(caseEntity));
    }

    @Test
    void formatsFixedPlusContingentRatio() {
        Case caseEntity = caseWithMethod("BASE_PLUS_CONTINGENT");
        caseEntity.setAttorneyFee(new BigDecimal("30000.00"));
        caseEntity.setRiskRatio(new BigDecimal("2.00"));

        assertEquals("固定收费 30000元+风险收费 2%", CaseFeeFormatter.format(caseEntity));
    }

    private Case caseWithMethod(String feeMethod) {
        Case caseEntity = new Case();
        caseEntity.setFeeMethod(feeMethod);
        return caseEntity;
    }
}
