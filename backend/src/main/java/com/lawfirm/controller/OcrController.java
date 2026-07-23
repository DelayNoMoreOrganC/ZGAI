package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.OcrHealthDTO;
import com.lawfirm.service.OcrService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

@Slf4j
@RestController
@RequestMapping("/ocr")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OcrController {
    private final OcrService ocrService;

    @PostMapping("/recognize")
    @AuditLog(value = "本地OCR识别", operationType = "READ", logParams = false)
    public Result<String> recognizeDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "document") String type) {
        try {
            String result = ocrService.recognizeDocument(file, type);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            return Result.validationError(e.getMessage());
        } catch (Exception e) {
            log.error("本地OCR识别失败", e);
            return Result.error("OCR识别失败，请检查本地OCR服务状态");
        }
    }

    @GetMapping("/health")
    public Result<OcrHealthDTO> health() {
        return Result.success(ocrService.getHealth());
    }
}
