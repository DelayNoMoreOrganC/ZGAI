package com.lawfirm.service;

import com.lawfirm.dto.OcrHealthDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "docx", "txt", "md", "png", "jpg", "jpeg");

    private final LocalDocumentTextService documentTextService;

    @Value("${ai.ocr.max-file-size:52428800}")
    private long maxFileSize;

    public String recognizeDocument(MultipartFile file, String type) throws Exception {
        validate(file, type);
        String suffix = resolveSuffix(file.getOriginalFilename(), file.getContentType());
        Path tempFile = Files.createTempFile("zgai-ocr-", suffix);
        try {
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("开始本地OCR/文本提取: size={}, mimeType={}, documentType={}",
                    file.getSize(), safeMetadata(file.getContentType()), safeMetadata(type));
            String text = documentTextService.extract(
                    tempFile, file.getOriginalFilename(), file.getContentType());
            if (!StringUtils.hasText(text)) {
                throw new IllegalArgumentException("未识别到可用文字，请检查文件清晰度或OCR语言配置");
            }
            return text;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public OcrHealthDTO getHealth() {
        return documentTextService.getHealth();
    }

    private void validate(MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("OCR文件超过系统允许大小");
        }
        if (StringUtils.hasText(type) && type.trim().length() > 40) {
            throw new IllegalArgumentException("文档类型不能超过40个字符");
        }
        resolveSuffix(file.getOriginalFilename(), file.getContentType());
    }

    private String resolveSuffix(String originalName, String mimeType) {
        String extension = extension(originalName);
        if (ALLOWED_EXTENSIONS.contains(extension)) {
            return "." + extension;
        }
        if (StringUtils.hasText(extension)) {
            throw new IllegalArgumentException("仅支持 PDF、DOCX、TXT、MD、PNG、JPG 文件");
        }
        if (mimeType != null) {
            String normalizedMimeType = mimeType.toLowerCase(Locale.ROOT);
            if ("application/pdf".equals(normalizedMimeType)) return ".pdf";
            if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(normalizedMimeType)) {
                return ".docx";
            }
            if (normalizedMimeType.startsWith("text/")) return ".txt";
            if ("image/png".equals(normalizedMimeType)) return ".png";
            if ("image/jpeg".equals(normalizedMimeType)) return ".jpg";
        }
        throw new IllegalArgumentException("仅支持 PDF、DOCX、TXT、MD、PNG、JPG 文件");
    }

    private String extension(String name) {
        if (!StringUtils.hasText(name) || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String safeMetadata(String value) {
        if (!StringUtils.hasText(value)) return "unknown";
        String normalized = value.trim().replaceAll("[^a-zA-Z0-9_+./-]", "");
        return normalized.substring(0, Math.min(80, normalized.length()));
    }
}
