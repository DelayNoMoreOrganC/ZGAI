package com.lawfirm.controller;

import com.lawfirm.dto.DocGenerateRequest;
import com.lawfirm.entity.User;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.service.DocGenerateService;
import com.lawfirm.util.Result;
import com.lawfirm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasAnyRole('ADMIN', 'LAWYER')")
    public Result<String> generateDocument(@RequestBody DocGenerateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        String result = docGenerateService.generateDocument(request, userId);
        return Result.success(result);
    }
}
