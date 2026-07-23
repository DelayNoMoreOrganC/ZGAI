package com.lawfirm.controller;

import com.lawfirm.dto.DocGenerateRequest;
import com.lawfirm.service.DocGenerateService;
import com.lawfirm.util.Result;
import com.lawfirm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 文书生成控制器
 */
@RestController
@RequestMapping("ai/generate-doc")
@RequiredArgsConstructor
public class DocGenerateController {

    private final DocGenerateService docGenerateService;
    private final SecurityUtil securityUtil;

    /**
     * 生成法律文书
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<String> generateDocument(@Valid @RequestBody DocGenerateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        String result = docGenerateService.generateDocument(request, userId);
        return Result.success(result);
    }
}
