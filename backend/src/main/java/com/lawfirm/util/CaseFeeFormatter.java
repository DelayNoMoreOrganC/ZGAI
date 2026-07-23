package com.lawfirm.util;

import com.lawfirm.entity.Case;

import java.math.BigDecimal;

/**
 * Formats a case's registered fee arrangement for approval and case views.
 */
public final class CaseFeeFormatter {

    private CaseFeeFormatter() {
    }

    public static String format(Case caseEntity) {
        if (caseEntity == null || caseEntity.getFeeMethod() == null) {
            return "";
        }
        switch (caseEntity.getFeeMethod()) {
            case "FIXED":
                return appendAmount("固定收费", caseEntity.getAttorneyFee());
            case "CONTINGENT":
                return formatRiskFee(caseEntity, "风险收费");
            case "BASE_PLUS_CONTINGENT":
            case "FIXED_PLUS_CONTINGENT":
                return appendAmount("固定收费", caseEntity.getAttorneyFee())
                        + "+" + formatRiskFee(caseEntity, "风险收费");
            case "FREE":
                return "免费代理";
            case "UNDETERMINED":
                return "未确定";
            case "OTHER":
                return hasText(caseEntity.getFeeNotes()) ? "其他：" + caseEntity.getFeeNotes().trim() : "其他";
            default:
                return caseEntity.getFeeMethod();
        }
    }

    private static String formatRiskFee(Case caseEntity, String label) {
        boolean hasAmount = isPositive(caseEntity.getRiskFee());
        boolean hasRatio = isPositive(caseEntity.getRiskRatio());
        if (hasAmount && hasRatio) {
            return label + " " + number(caseEntity.getRiskFee()) + "元（" + number(caseEntity.getRiskRatio()) + "%）";
        }
        if (hasRatio) {
            return label + " " + number(caseEntity.getRiskRatio()) + "%";
        }
        return appendAmount(label, caseEntity.getRiskFee());
    }

    private static String appendAmount(String label, BigDecimal amount) {
        return isPositive(amount) ? label + " " + number(amount) + "元" : label;
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static String number(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
