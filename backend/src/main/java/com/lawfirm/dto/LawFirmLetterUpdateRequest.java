package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class LawFirmLetterUpdateRequest {
    @NotBlank(message = "收函单位不能为空")
    @Size(max = 300, message = "收函单位不能超过300个字符")
    private String recipient;

    @NotBlank(message = "委托客户不能为空")
    @Size(max = 500, message = "委托客户不能超过500个字符")
    private String clientName;

    @NotBlank(message = "办案人不能为空")
    @Size(max = 1000, message = "办案人不能超过1000个字符")
    private String lawyerNames;

    @NotBlank(message = "相对方不能为空")
    @Size(max = 1000, message = "相对方不能超过1000个字符")
    private String opposingParty;

    @NotBlank(message = "案由不能为空")
    @Size(max = 500, message = "案由不能超过500个字符")
    private String caseReason;

    @NotBlank(message = "函种不能为空")
    @Pattern(regexp = "[民刑行仲非顾案]", message = "函种只能使用民、刑、行、仲、非、顾或案")
    private String letterTypeCode;

    @NotBlank(message = "办案人联系电话不能为空")
    @Size(max = 1000, message = "办案人联系电话不能超过1000个字符")
    private String lawyerContacts;

    @NotBlank(message = "结尾用语不能为空")
    @Size(max = 100, message = "结尾用语不能超过100个字符")
    private String closingText;

    private LocalDate issueDate;
    private Long lockVersion;
}
