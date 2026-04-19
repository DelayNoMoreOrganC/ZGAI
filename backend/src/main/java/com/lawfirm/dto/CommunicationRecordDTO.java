package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 沟通记录DTO
 */
@Data
public class CommunicationRecordDTO {

    @NotBlank(message = "沟通方式不能为空")
    private String communicationType;

    @NotNull(message = "沟通时间不能为空")
    private LocalDate communicationDate;

    @NotBlank(message = "沟通内容不能为空")
    private String content;

    private LocalDate nextFollowDate;

    private String attachments;
}
