package com.lawfirm.controller;

import com.lawfirm.dto.LegacyMaterialSearchRequest;
import com.lawfirm.dto.LegacyMaterialSearchResponse;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.LegacyMaterialSearchService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 旧系统资料检索。
 */
@Slf4j
@RestController
@RequestMapping("/legacy-materials")
@RequiredArgsConstructor
public class LegacyMaterialSearchController {

    private final LegacyMaterialSearchService legacyMaterialSearchService;
    private final SecurityUtils securityUtils;

    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public Result<LegacyMaterialSearchResponse> search(@RequestBody LegacyMaterialSearchRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        return Result.success(legacyMaterialSearchService.search(request, currentUserId));
    }
}
