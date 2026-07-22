package com.lawfirm.service;

import com.lawfirm.dto.KnowledgeArticleDTO;
import com.lawfirm.dto.KnowledgeArticleVO;
import com.lawfirm.entity.KnowledgeArticle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class KnowledgeDocumentImportService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "docx", "txt", "md");

    private final KnowledgeArticleService articleService;
    private final Path knowledgeLibraryRoot;
    private final long maxFileSize;

    public KnowledgeDocumentImportService(
            KnowledgeArticleService articleService,
            @Value("${file.knowledge-library-root:./knowledge-files}") String knowledgeLibraryRoot,
            @Value("${file.max-size:52428800}") long maxFileSize) {
        this.articleService = articleService;
        this.knowledgeLibraryRoot = Paths.get(knowledgeLibraryRoot).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
    }

    public KnowledgeArticleVO importDocument(MultipartFile file, KnowledgeArticleDTO dto) {
        validateFile(file);
        String source = normalizeSource(dto.getKnowledgeSource());
        String originalName = safeOriginalName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        String content = extractText(file, extension);
        if (content.trim().isEmpty()) {
            if ("pdf".equals(extension)) {
                throw new IllegalArgumentException("PDF 未提取到文字，可能是扫描件，请先完成 OCR 后再导入");
            }
            throw new IllegalArgumentException("文档未提取到可用正文");
        }

        dto.setKnowledgeSource(source);
        dto.setTitle(resolveTitle(dto.getTitle(), originalName));
        dto.setArticleType(resolveArticleType(dto.getArticleType(), source));
        dto.setContent(content);
        Path storedFile = storeOriginal(file, source, originalName);
        try {
            return articleService.createImportedArticle(dto, storedFile.toString());
        } catch (RuntimeException e) {
            try {
                Files.deleteIfExists(storedFile);
            } catch (IOException ignored) {
                // 数据库失败时尽量清理文件；清理失败由运维检查知识库目录。
            }
            throw e;
        }
    }

    public ResponseEntity<Resource> downloadAttachment(Long articleId) {
        KnowledgeArticle article = articleService.getArticleForAttachment(articleId);
        if (article.getAttachmentPath() == null || article.getAttachmentPath().trim().isEmpty()) {
            throw new IllegalArgumentException("该知识条目没有原始附件");
        }
        Path file = Paths.get(article.getAttachmentPath()).toAbsolutePath().normalize();
        if (!file.startsWith(knowledgeLibraryRoot)) {
            throw new IllegalArgumentException("附件路径不在知识库目录内");
        }
        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new IllegalArgumentException("知识文档原件不存在或不可读取");
        }

        String downloadName = file.getFileName().toString().replaceFirst("^[0-9a-fA-F-]{36}_", "");
        Resource resource = new FileSystemResource(file);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(downloadName, StandardCharsets.UTF_8)
                .build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.toFile().length())
                .body(resource);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要导入的知识文档");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("文件大小超过系统限制");
        }
        String extension = extensionOf(safeOriginalName(file.getOriginalFilename()));
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 PDF、DOCX、TXT、MD 文档");
        }
    }

    private String extractText(MultipartFile file, String extension) {
        try (InputStream inputStream = file.getInputStream()) {
            String text;
            if ("pdf".equals(extension)) {
                try (PDDocument document = PDDocument.load(inputStream)) {
                    text = new PDFTextStripper().getText(document);
                }
            } else if ("docx".equals(extension)) {
                try (XWPFDocument document = new XWPFDocument(inputStream);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    text = extractor.getText();
                }
            } else {
                text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            return normalizeText(text);
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("文档解析失败，请确认文件未损坏且格式正确", e);
        }
    }

    private Path storeOriginal(MultipartFile file, String source, String originalName) {
        Path targetDirectory = knowledgeLibraryRoot
                .resolve(source)
                .resolve(String.valueOf(LocalDate.now().getYear()))
                .normalize();
        if (!targetDirectory.startsWith(knowledgeLibraryRoot)) {
            throw new IllegalArgumentException("知识库目录无效");
        }
        try {
            Files.createDirectories(targetDirectory);
            Path target = targetDirectory.resolve(UUID.randomUUID() + "_" + originalName).normalize();
            if (!target.startsWith(targetDirectory)) {
                throw new IllegalArgumentException("文件名无效");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } catch (IOException e) {
            throw new IllegalArgumentException("知识文档原件保存失败，请检查 NAS 连接和目录权限", e);
        }
    }

    private String normalizeSource(String source) {
        String normalized;
        try {
            normalized = KnowledgeArticlePolicy.normalizeSource(source);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("首期仅允许导入法律法规、律所制度、公共模板和参考资料");
        }
        if (!KnowledgeArticlePolicy.isSharedRagSource(normalized)
                || KnowledgeArticlePolicy.CASE_DEPOSIT.equals(normalized)) {
            throw new IllegalArgumentException("首期仅允许导入法律法规、律所制度、公共模板和参考资料");
        }
        return normalized;
    }

    private String resolveArticleType(String articleType, String source) {
        if ("PUBLIC_TEMPLATE".equals(source)) {
            return "TEMPLATE";
        }
        return articleType == null || articleType.trim().isEmpty() ? "DOCUMENT" : articleType.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveTitle(String title, String originalName) {
        if (title != null && !title.trim().isEmpty()) {
            return title.trim();
        }
        int dot = originalName.lastIndexOf('.');
        return dot > 0 ? originalName.substring(0, dot) : originalName;
    }

    private String safeOriginalName(String originalName) {
        String fileName = originalName == null ? "知识文档" : Paths.get(originalName).getFileName().toString();
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return fileName.isEmpty() ? "知识文档" : fileName;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\u0000", "")
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" +\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
