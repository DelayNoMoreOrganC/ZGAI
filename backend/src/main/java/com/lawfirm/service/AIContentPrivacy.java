package com.lawfirm.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

final class AIContentPrivacy {

    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)\\d{17}[0-9Xx](?!\\d)");
    private static final Pattern CREDIT_CODE = Pattern.compile("(?<![0-9A-Z])[0-9A-HJ-NPQRTUWXY]{18}(?![0-9A-Z])", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1\\d{10}(?!\\d)");
    private static final Pattern LANDLINE = Pattern.compile("(?<!\\d)0\\d{2,3}[- ]?\\d{7,8}(?!\\d)");
    private static final Pattern EMAIL = Pattern.compile("(?i)(?<![\\w.+-])[\\w.+-]+@[\\w.-]+\\.[A-Z]{2,}(?![\\w.-])");
    private static final Pattern BANK_CARD = Pattern.compile("(?<!\\d)(?:\\d[ -]?){15,18}\\d(?!\\d)");

    private AIContentPrivacy() {
    }

    static String summarize(String content) {
        if (content == null || content.trim().isEmpty()) return null;
        String value = ID_CARD.matcher(content).replaceAll("[证件号已脱敏]");
        value = CREDIT_CODE.matcher(value).replaceAll("[信用代码已脱敏]");
        value = PHONE.matcher(value).replaceAll("[手机号已脱敏]");
        value = LANDLINE.matcher(value).replaceAll("[联系电话已脱敏]");
        value = EMAIL.matcher(value).replaceAll("[邮箱已脱敏]");
        value = BANK_CARD.matcher(value).replaceAll("[银行卡号已脱敏]");
        value = value.replaceAll("\\s+", " ").trim();
        return value.length() <= 180 ? value : value.substring(0, 180) + "...";
    }

    static String sha256(String content) {
        if (content == null) return null;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("AI内容摘要计算失败", e);
        }
    }

    static String commandSummary(String instruction) {
        if (instruction == null || instruction.trim().isEmpty()) return "指令内容已移除（字符数：0）";
        int characterCount = instruction.codePointCount(0, instruction.length());
        return "指令内容已移除（字符数：" + characterCount + "）";
    }

    static String errorSummary(String errorMessage) {
        return errorMessage == null || errorMessage.trim().isEmpty() ? null : "AI调用失败";
    }
}
