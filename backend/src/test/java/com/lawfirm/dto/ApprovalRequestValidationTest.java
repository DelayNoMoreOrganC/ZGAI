package com.lawfirm.dto;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ApprovalRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void queryCapsPageSizeAndWhitelistsSortField() {
        ApprovalQueryRequest request = new ApprovalQueryRequest();
        request.setPage(0);
        request.setSize(1000);
        request.setSortField("caseEntity.secret");
        request.setSortDirection("SIDEWAYS");

        assertThat(messages(validator.validate(request))).contains(
                "页码必须从1开始", "每页数量不能超过100", "不支持的排序字段", "排序方向只能是ASC或DESC");
    }

    @Test
    void decisionAndTransferBodiesHaveBoundedTextAndRequiredApprover() {
        ApprovalDecisionRequest decision = new ApprovalDecisionRequest();
        decision.setComments("a".repeat(2001));
        assertThat(messages(validator.validate(decision))).contains("审批意见不能超过2000个字符");

        ApprovalTransferRequest transfer = new ApprovalTransferRequest();
        transfer.setComments("b".repeat(2001));
        assertThat(messages(validator.validate(transfer)))
                .contains("新审批人不能为空", "转审备注不能超过2000个字符");
    }

    @Test
    void createBodyHasBoundedBusinessFields() {
        ApprovalCreateRequest request = new ApprovalCreateRequest();
        request.setApprovalType("a".repeat(41));
        request.setTitle("b".repeat(201));
        request.setContent("c".repeat(5001));
        request.setAttachments("d".repeat(2001));
        request.setCurrentApproverId(8L);

        assertThat(messages(validator.validate(request))).contains(
                "审批类型不能超过40个字符", "审批标题不能超过200个字符",
                "审批内容不能超过5000个字符", "附件信息不能超过2000个字符");
    }

    @Test
    void queryValidatesStatusGroupAndDateRange() {
        ApprovalQueryRequest request = new ApprovalQueryRequest();
        request.setStatus("UNKNOWN");
        request.setStatusGroup("EVERYTHING");
        request.setStartDate(LocalDate.of(2026, 7, 24));
        request.setEndDate(LocalDate.of(2026, 7, 23));

        assertThat(messages(validator.validate(request))).contains(
                "不支持的审批状态", "不支持的审批状态分组", "申请开始日期不能晚于结束日期");
    }

    private Set<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }
}
