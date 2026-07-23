package com.lawfirm.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.time.LocalDateTime;

@Data
public class AIDocumentIntakeConfirmRequest {
    @NotNull(message = "请选择案件")
    private Long caseId;

    @NotBlank(message = "请选择案件目录")
    @Size(max = 100, message = "案件目录不能超过100个字符")
    private String folderPath;

    @NotBlank(message = "请选择文件类型")
    @Size(max = 20, message = "文件类型不能超过20个字符")
    private String documentType;

    private Boolean registerActivity = true;

    private Boolean createHearingCalendar = false;

    private LocalDateTime hearingTime;

    @Size(max = 200, message = "开庭地点不能超过200个字符")
    private String hearingLocation;

    private Boolean createDeadlineTodo = false;

    private LocalDateTime deadlineTime;

    @Size(max = 200, message = "期限待办标题不能超过200个字符")
    private String deadlineTitle;
}
