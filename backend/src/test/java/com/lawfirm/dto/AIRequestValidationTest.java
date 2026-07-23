package com.lawfirm.dto;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AIRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void rejectsOversizedChatMessage() {
        AiChatRequest request = new AiChatRequest();
        request.setMessage("a".repeat(20001));

        assertThat(messages(validator.validate(request)))
                .contains("消息内容不能超过20000个字符");
    }

    @Test
    void rejectsOversizedCaseCommandAndIdempotencyKey() {
        AICaseCommandRequest request = new AICaseCommandRequest();
        request.setInstruction("a".repeat(4001));
        request.setIdempotencyKey("b".repeat(81));

        assertThat(messages(validator.validate(request)))
                .contains("指令不能超过4000个字符", "幂等键不能超过80个字符");
    }

    @Test
    void rejectsOversizedDocumentGenerationContext() {
        DocGenerateRequest request = new DocGenerateRequest();
        request.setCaseId(1L);
        request.setDocumentType("COMPLAINT");
        request.setAdditionalContext("a".repeat(20001));

        assertThat(messages(validator.validate(request)))
                .contains("补充材料不能超过20000个字符");
    }

    @Test
    void rejectsBlankOrOversizedOcrExtractionInput() {
        OcrExtractRequest blank = new OcrExtractRequest();
        blank.setOcrText("   ");
        assertThat(messages(validator.validate(blank))).contains("OCR文本不能为空");

        OcrExtractRequest oversized = new OcrExtractRequest();
        oversized.setOcrText("a".repeat(60001));
        oversized.setDocumentType("b".repeat(41));
        assertThat(messages(validator.validate(oversized)))
                .contains("OCR文本不能超过60000个字符", "文书类型不能超过40个字符");
    }

    private Set<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }
}
