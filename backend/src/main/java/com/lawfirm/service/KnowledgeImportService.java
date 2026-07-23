package com.lawfirm.service;

import com.lawfirm.dto.KnowledgeImportConfirmRequest;
import com.lawfirm.dto.KnowledgeReviewRequest;
import com.lawfirm.entity.KnowledgeArticle;
import com.lawfirm.entity.KnowledgeImportBatch;
import com.lawfirm.entity.KnowledgeImportItem;
import com.lawfirm.entity.User;
import com.lawfirm.repository.KnowledgeArticleRepository;
import com.lawfirm.repository.KnowledgeImportBatchRepository;
import com.lawfirm.repository.KnowledgeImportItemRepository;
import com.lawfirm.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class KnowledgeImportService {

    private static final Set<String> SUPPORTED = Set.of("pdf", "doc", "docx", "txt", "md");
    private static final String FLK_HOST = "flk.npc.gov.cn";
    private static final String FLK_DETAIL_API = "https://flk.npc.gov.cn/law-search/search/flfgDetails?bbbs=";
    private static final String FLK_DOWNLOAD_API = "https://flk.npc.gov.cn/law-search/download/pc?format=%s&fileId=&bbbs=%s";
    private static final List<String> STARTER_FLK_IDS = List.of(
            "ff808081729d1efe01729d50b5c500bf", // 民法典
            "ff8081818a21dc13018b425303b7086d", // 民事诉讼法（2023修正）
            "ff80818181cdceb30181d801c31c4070", // 民事诉讼法司法解释
            "ff808181796a636a0179822a19640c92", // 刑法
            "ff8080816f135f46016f1d1b81b01351", // 刑事诉讼法
            "2c909fdd678bf17901678bf858550a0f", // 行政诉讼法
            "ff8081818a21e6c3018a508d491a0c98", // 行政复议法
            "58d7569a322b4eca9b22feaa4f5d7d4f", // 仲裁法（2025修订）
            "ff8081818c9108eb018cb6922f750c07", // 公司法（2023修订）
            "2c909fdd678bf17901678bf74d7106b3", // 劳动合同法
            "2c909fdd678bf17901678bf867e80a55"  // 律师法
    );

    private final KnowledgeImportBatchRepository batchRepository;
    private final KnowledgeImportItemRepository itemRepository;
    private final KnowledgeArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final VectorMigrationService vectorMigrationService;
    private final LocalDocumentTextService documentTextService;
    private final ObjectMapper objectMapper;

    @Value("${knowledge.import.firm-policy-source-root:${FIRM_POLICY_SOURCE_ROOT:}}")
    private String policySourceRoot;
    @Value("${knowledge.import.staging-root:./data/knowledge-imports}")
    private String stagingRoot;
    @Value("${file.knowledge-library-root:./knowledge-files}")
    private String libraryRoot;
    @Value("${knowledge.import.max-file-size-bytes:52428800}")
    private long maxFileSizeBytes;

    @Transactional
    public KnowledgeImportBatch previewFlk(List<String> urls, Long userId) {
        List<String> unique = urls.stream().filter(StringUtils::hasText).map(String::trim)
                .distinct().collect(Collectors.toList());
        if (unique.isEmpty() || unique.size() > 50) throw new IllegalArgumentException("单批须提交1至50个法规详情链接");
        for (String value : unique) validateFlkUrl(value);
        KnowledgeImportBatch batch = newBatch("FLK", "DISCOVERED", userId, unique.size());
        for (String url : unique) {
            KnowledgeImportItem item = new KnowledgeImportItem();
            item.setBatchId(batch.getId());
            item.setSourceUrl(url);
            item.setTitle(titleFromUrl(url));
            item.setStatus(itemRepository.existsBySourceUrlAndDeletedFalse(url) ? "FAILED" : "DISCOVERED");
            if ("FAILED".equals(item.getStatus())) item.setErrorMessage("该官方链接已存在于导入记录中");
            itemRepository.save(item);
        }
        return batch;
    }

    @Transactional
    public KnowledgeImportBatch createStarterFlkBatch(Long userId) {
        List<String> urls = STARTER_FLK_IDS.stream()
                .map(id -> "https://flk.npc.gov.cn/detail?id=" + id)
                .collect(Collectors.toList());
        return previewFlk(urls, userId);
    }

    @Transactional
    public KnowledgeImportBatch stageFlk(Long batchId) {
        KnowledgeImportBatch batch = requireBatch(batchId, "FLK");
        for (KnowledgeImportItem item : items(batchId)) {
            boolean retryableFailure = "FAILED".equals(item.getStatus())
                    && item.getArticleId() == null
                    && StringUtils.hasText(item.getStagedPath());
            boolean refreshPendingPdf = "PENDING_REVIEW".equals(item.getStatus())
                    && item.getArticleId() != null
                    && !"docx".equals(extension(item.getOriginalFileName()));
            if (!"DISCOVERED".equals(item.getStatus()) && !retryableFailure && !refreshPendingPdf) continue;
            try {
                fetchOfficialResource(item);
                if (refreshPendingPdf) refreshPendingArticle(item);
            } catch (Exception e) {
                if (refreshPendingPdf) {
                    item.setStatus("PENDING_REVIEW");
                    item.setErrorMessage("官方DOCX刷新失败，已保留原待审核条目：" + safeMessage(e.getMessage()));
                } else {
                    item.setStatus("NEEDS_UPLOAD");
                    item.setErrorMessage(safeMessage(e.getMessage()));
                }
            }
            itemRepository.save(item);
            sleepQuietly(400);
        }
        batch.setStatus(resolveBatchStatus(items(batchId)));
        return batchRepository.save(batch);
    }

    private void refreshPendingArticle(KnowledgeImportItem item) throws Exception {
        KnowledgeArticle article = articleRepository.findById(item.getArticleId())
                .orElseThrow(() -> new IllegalArgumentException("待审核知识条目不存在"));
        if (!"PENDING_REVIEW".equals(article.getReviewStatus())) {
            throw new IllegalArgumentException("仅待审核知识可以刷新官方文件");
        }
        Path source = sourcePath(item);
        String content = extractText(source);
        if (!StringUtils.hasText(content)) throw new IllegalArgumentException("官方DOCX未提取到可用正文");

        Path targetRoot = Paths.get(libraryRoot).toAbsolutePath().normalize()
                .resolve(KnowledgeArticlePolicy.LAW_REGULATION);
        Files.createDirectories(targetRoot);
        Path target = targetRoot.resolve(UUID.randomUUID() + "_" + safeName(source.getFileName().toString())).normalize();
        if (!target.startsWith(targetRoot)) throw new IllegalArgumentException("知识库目标路径不合法");
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        deleteOnRollback(target);

        String previousAttachment = article.getAttachmentPath();
        try {
            article.setContent(content);
            article.setSummary(content.length() > 300 ? content.substring(0, 300) : content);
            article.setAttachmentPath(target.toString());
            article.setContentSha256(item.getContentSha256());
            article.setIssuingAuthority(item.getIssuingAuthority());
            article.setEffectiveDate(item.getEffectiveDate());
            article.setValidityStatus(item.getValidityStatus());
            article.setCollectedAt(item.getCollectedAt());
            articleRepository.save(article);
            item.setStatus("PENDING_REVIEW");
            item.setErrorMessage(null);
            deletePreviousAttachmentAfterCommit(previousAttachment, targetRoot, target);
        } catch (Exception e) {
            Files.deleteIfExists(target);
            throw e;
        }
    }

    @Transactional
    public KnowledgeImportItem attach(Long itemId, MultipartFile file) {
        KnowledgeImportItem item = requireItem(itemId);
        requireBatch(item.getBatchId(), "FLK");
        if (!"NEEDS_UPLOAD".equals(item.getStatus())) {
            throw new IllegalArgumentException("仅需补传的法规项目可以上传官方附件");
        }
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择官方附件");
        if (file.getSize() > maxFileSizeBytes) throw new IllegalArgumentException("官方附件不得超过50MB");
        String name = safeName(file.getOriginalFilename());
        requireSupported(name);
        try {
            Path target = itemStagingPath(item, name);
            Files.createDirectories(target.getParent());
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            item.setOriginalFileName(name);
            item.setStagedPath(target.toString());
            item.setContentSha256(sha256(target));
            item.setStatus("STAGED");
            item.setErrorMessage(null);
            return itemRepository.save(item);
        } catch (IOException e) {
            throw new IllegalArgumentException("官方附件暂存失败", e);
        }
    }

    @Transactional
    public KnowledgeImportBatch scanPolicies(Long userId) {
        if (!StringUtils.hasText(policySourceRoot)) throw new IllegalStateException("尚未配置 FIRM_POLICY_SOURCE_ROOT");
        Path root = Paths.get(policySourceRoot).toAbsolutePath().normalize();
        if (!Files.isDirectory(root) || !Files.isReadable(root)) throw new IllegalStateException("律所制度目录不可读取");
        List<Path> files;
        try (Stream<Path> stream = Files.walk(root)) {
            files = stream.filter(Files::isRegularFile)
                    .filter(path -> isAcceptedPolicyFile(root, path))
                    .sorted().collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("律所制度目录扫描失败", e);
        }
        KnowledgeImportBatch batch = newBatch("FIRM_POLICY", "DISCOVERED", userId, files.size());
        for (Path file : files) {
            KnowledgeImportItem item = new KnowledgeImportItem();
            item.setBatchId(batch.getId());
            item.setStatus("DISCOVERED");
            item.setTitle(stripExtension(file.getFileName().toString()));
            item.setOriginalFileName(file.getFileName().toString());
            item.setSourceRelativePath(root.relativize(file).toString());
            item.setSourceAbsolutePath(file.toString());
            try {
                item.setContentSha256(sha256(file));
                if (itemRepository.findFirstByContentSha256AndArticleIdIsNotNullAndDeletedFalse(item.getContentSha256()).isPresent()) {
                    item.setStatus("FAILED");
                    item.setErrorMessage("相同内容已导入知识库");
                }
            } catch (IOException e) {
                item.setStatus("FAILED");
                item.setErrorMessage("文件读取失败");
            }
            itemRepository.save(item);
        }
        return batch;
    }

    @Transactional
    public KnowledgeImportBatch confirm(Long batchId, KnowledgeImportConfirmRequest request, Long userId) {
        KnowledgeImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("导入批次不存在"));
        Set<Long> selected = request == null || request.getItemIds() == null || request.getItemIds().isEmpty()
                ? Collections.emptySet() : new HashSet<>(request.getItemIds());
        int processed = 0;
        for (KnowledgeImportItem item : items(batchId)) {
            if (!selected.isEmpty() && !selected.contains(item.getId())) continue;
            boolean confirmable = "STAGED".equals(item.getStatus())
                    || ("FIRM_POLICY".equals(batch.getSourceType()) && "DISCOVERED".equals(item.getStatus()));
            if (!confirmable) continue;
            try {
                createPendingArticle(batch, item, userId);
                processed++;
            } catch (ConversionRequiredException e) {
                item.setStatus("CONVERSION_REQUIRED");
                item.setErrorMessage(e.getMessage());
            } catch (Exception e) {
                item.setStatus("FAILED");
                item.setErrorMessage(safeMessage(e.getMessage()));
            }
            itemRepository.save(item);
        }
        if (processed == 0) {
            throw new IllegalArgumentException("没有可生成的条目；法规请先完成暂存或补传官方文件");
        }
        batch.setStatus(resolveBatchStatus(items(batchId)));
        return batchRepository.save(batch);
    }

    @Transactional
    public KnowledgeArticle review(Long articleId, KnowledgeReviewRequest request, Long reviewerId) {
        KnowledgeArticle article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("知识条目不存在"));
        if (!"PENDING_REVIEW".equals(article.getReviewStatus())) {
            throw new IllegalArgumentException("该知识条目已经完成审核，不能重复修改结论");
        }
        String decision = request.getDecision().trim().toUpperCase(Locale.ROOT);
        if (!("APPROVED".equals(decision) || "REJECTED".equals(decision))) {
            throw new IllegalArgumentException("审核结论仅支持 APPROVED 或 REJECTED");
        }
        if ("REJECTED".equals(decision) && !StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("驳回知识条目必须填写原因");
        }
        article.setReviewStatus(decision);
        article.setReviewedBy(reviewerId);
        article.setReviewedAt(LocalDateTime.now());
        article.setReviewReason(request.getReason());
        boolean approved = "APPROVED".equals(decision);
        article.setIsPublic(approved);
        boolean eligible = approved && !KnowledgeArticlePolicy.VALIDITY_REPEALED.equals(article.getValidityStatus());
        article.setKnowledgeEligible(eligible);
        article.setIndexStatus(eligible ? "PENDING" : "FORBIDDEN");
        KnowledgeArticle saved = articleRepository.save(article);
        itemRepository.findFirstByArticleIdAndDeletedFalse(articleId).ifPresent(item -> {
            item.setStatus(decision);
            item.setErrorMessage("REJECTED".equals(decision) ? safeMessage(request.getReason()) : null);
            itemRepository.save(item);
            batchRepository.findById(item.getBatchId()).ifPresent(batch -> {
                batch.setStatus(resolveBatchStatus(items(batch.getId())));
                batchRepository.save(batch);
            });
        });
        afterCommit(() -> {
            if (approved) vectorMigrationService.indexNewArticle(saved);
            else vectorMigrationService.deleteArticleIndex(saved.getId());
        });
        return saved;
    }

    public List<KnowledgeImportBatch> listBatches() { return batchRepository.findTop30ByDeletedFalseOrderByCreatedAtDesc(); }
    public List<KnowledgeImportItem> items(Long batchId) { return itemRepository.findByBatchIdAndDeletedFalseOrderByIdAsc(batchId); }

    private void createPendingArticle(KnowledgeImportBatch batch, KnowledgeImportItem item, Long userId) throws Exception {
        if (StringUtils.hasText(item.getContentSha256())
                && articleRepository.findFirstByContentSha256AndDeletedFalse(item.getContentSha256()).isPresent()) {
            throw new IllegalArgumentException("相同内容已存在于知识库");
        }
        Path source = sourcePath(item);
        Path parseSource = "doc".equals(extension(source.getFileName().toString())) ? convertDoc(source) : source;
        String content = extractText(parseSource);
        if (!StringUtils.hasText(content)) throw new IllegalArgumentException("文档自动解析或OCR后仍未提取到可用正文");
        String sourceType = "FLK".equals(batch.getSourceType())
                ? KnowledgeArticlePolicy.LAW_REGULATION : KnowledgeArticlePolicy.FIRM_POLICY;
        Path targetRoot = Paths.get(libraryRoot).toAbsolutePath().normalize().resolve(sourceType);
        Files.createDirectories(targetRoot);
        String targetName = UUID.randomUUID() + "_" + safeName(source.getFileName().toString());
        Path target = targetRoot.resolve(targetName).normalize();
        if (!target.startsWith(targetRoot)) throw new IllegalArgumentException("知识库目标路径不合法");
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        deleteOnRollback(target);

        try {
            User author = userRepository.findById(userId).orElse(null);
            KnowledgeArticle article = new KnowledgeArticle();
            article.setTitle(StringUtils.hasText(item.getTitle()) ? item.getTitle() : stripExtension(source.getFileName().toString()));
            article.setArticleType("DOCUMENT");
            article.setKnowledgeSource(sourceType);
            article.setCategory("FLK".equals(batch.getSourceType()) ? "法律法规" : "律所制度");
            article.setContent(content);
            article.setSummary(content.length() > 300 ? content.substring(0, 300) : content);
            article.setAttachmentPath(target.toString());
            article.setSourceReference(item.getSourceUrl() != null ? item.getSourceUrl() : item.getSourceRelativePath());
            article.setSourceUrl(item.getSourceUrl());
            article.setSourceRelativePath(item.getSourceRelativePath());
            article.setContentSha256(StringUtils.hasText(item.getContentSha256()) ? item.getContentSha256() : sha256(source));
            article.setIssuingAuthority(item.getIssuingAuthority());
            article.setDocumentNumber(item.getDocumentNumber());
            article.setEffectiveDate(item.getEffectiveDate());
            article.setValidityStatus(item.getValidityStatus());
            article.setCollectedAt(item.getCollectedAt());
            article.setReviewStatus("PENDING_REVIEW");
            article.setIsPublic(false);
            article.setKnowledgeEligible(false);
            article.setIndexStatus("FORBIDDEN");
            article.setAuthorId(userId);
            article.setAuthorName(author == null ? "知识导入" : author.getRealName());
            article = articleRepository.save(article);
            item.setArticleId(article.getId());
            item.setStatus("PENDING_REVIEW");
            item.setErrorMessage(null);
        } catch (Exception e) {
            Files.deleteIfExists(target);
            throw e;
        }
    }

    private void fetchOfficialResource(KnowledgeImportItem item) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        String flkId = flkId(item.getSourceUrl());
        if (StringUtils.hasText(flkId)) {
            fetchFlkDetailResource(client, item, flkId);
            return;
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(item.getSourceUrl()))
                .timeout(Duration.ofSeconds(20)).header("User-Agent", "ZGAI-Knowledge-Importer/1.0")
                .GET().build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        requireAllowedOfficialResponse(response.uri(), false);
        if (response.statusCode() != 200) throw new IOException("官方网站返回HTTP " + response.statusCode() + "，请人工下载后补传");
        String contentType = response.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
        if (contentType.contains("text/html")) {
            item.setStatus("NEEDS_UPLOAD");
            item.setErrorMessage("详情页未提供可直接下载的公开附件，请人工下载官方文件后补传");
            return;
        }
        String name = fileNameFromUrl(item.getSourceUrl(), contentType);
        requireSupported(name);
        Path target = itemStagingPath(item, name);
        Files.createDirectories(target.getParent());
        Files.write(target, response.body());
        item.setOriginalFileName(name);
        item.setStagedPath(target.toString());
        item.setContentSha256(sha256(target));
        item.setStatus("STAGED");
        item.setErrorMessage(null);
    }

    private void fetchFlkDetailResource(HttpClient client, KnowledgeImportItem item, String flkId) throws Exception {
        JsonNode detail = getOfficialJson(client, FLK_DETAIL_API + encode(flkId)).path("data");
        if (detail.isMissingNode() || detail.isNull()) {
            throw new IOException("官方网站未返回法规详情，请人工下载后补传");
        }
        item.setTitle(textValue(detail, "title", item.getTitle()));
        item.setIssuingAuthority(textValue(detail, "zdjgName", null));
        item.setPublishedDate(dateValue(detail.path("gbrq").asText(null)));
        item.setEffectiveDate(dateValue(detail.path("sxrq").asText(null)));
        item.setValidityStatus(validityStatus(detail.path("sxx").asInt(0)));

        IOException docxFailure = null;
        try {
            if (downloadOfficialFile(client, item, flkId, "docx")) return;
        } catch (IOException e) {
            docxFailure = e;
        }
        try {
            if (downloadOfficialFile(client, item, flkId, "pdf")) return;
        } catch (IOException e) {
            if (docxFailure != null) e.addSuppressed(docxFailure);
            throw e;
        }
        throw new IOException("官方网站未返回可下载的DOCX或PDF，请人工下载后补传");
    }

    private boolean downloadOfficialFile(HttpClient client, KnowledgeImportItem item, String flkId,
                                         String format) throws Exception {
        String apiUrl = String.format(FLK_DOWNLOAD_API, encode(format), encode(flkId));
        JsonNode download = getOfficialJson(client, apiUrl).path("data");
        String downloadUrl = download.path("url").asText(null);
        if (!StringUtils.hasText(downloadUrl)) return false;
        URI uri = URI.create(downloadUrl);
        requireAllowedOfficialResponse(uri, true);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60))
                .header("User-Agent", "ZGAI-Knowledge-Importer/1.0").GET().build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        requireAllowedOfficialResponse(response.uri(), true);
        if (response.statusCode() != 200) {
            throw new IOException("官方附件下载返回HTTP " + response.statusCode() + "，请人工下载后补传");
        }
        if (response.body().length == 0 || response.body().length > maxFileSizeBytes) {
            throw new IOException("官方附件为空或超过50MB，请人工下载后补传");
        }
        String contentType = response.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
        boolean pdf = "pdf".equals(format);
        boolean expectedContent = pdf
                ? contentType.contains("pdf") || hasPdfSignature(response.body())
                : contentType.contains("openxmlformats") || hasZipSignature(response.body());
        if (!expectedContent) {
            throw new IOException("官方网站返回的附件格式与请求不一致，请人工下载后补传");
        }
        String name = safeName(item.getTitle())
                + (item.getPublishedDate() == null ? "" : "_" + item.getPublishedDate())
                + "." + format;
        Path target = itemStagingPath(item, name);
        Files.createDirectories(target.getParent());
        Files.write(target, response.body());
        item.setOriginalFileName(name);
        item.setStagedPath(target.toString());
        item.setContentSha256(sha256(target));
        item.setStatus("STAGED");
        item.setErrorMessage(null);
        return true;
    }

    private JsonNode getOfficialJson(HttpClient client, String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("User-Agent", "ZGAI-Knowledge-Importer/1.0").GET().build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        requireAllowedOfficialResponse(response.uri(), false);
        if (response.statusCode() != 200) {
            throw new IOException("官方网站返回HTTP " + response.statusCode() + "，请人工下载后补传");
        }
        JsonNode root = objectMapper.readTree(response.body());
        if (root.path("code").asInt(0) != 200) {
            throw new IOException(safeMessage(root.path("msg").asText("官方网站接口暂不可用")));
        }
        return root;
    }

    private Path sourcePath(KnowledgeImportItem item) {
        String value = StringUtils.hasText(item.getStagedPath()) ? item.getStagedPath() : item.getSourceAbsolutePath();
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException("导入项没有可用文件");
        Path path = Paths.get(value).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) throw new IllegalArgumentException("源文件不可读取");
        if (!StringUtils.hasText(item.getStagedPath())) {
            Path root = Paths.get(policySourceRoot).toAbsolutePath().normalize();
            if (!path.startsWith(root)) throw new IllegalArgumentException("制度文件路径越界");
        }
        return path;
    }

    private Path convertDoc(Path source) throws Exception {
        Path output = Paths.get(stagingRoot).toAbsolutePath().normalize().resolve("converted");
        Files.createDirectories(output);
        Process process;
        try {
            process = new ProcessBuilder("soffice", "--headless", "--convert-to", "docx", "--outdir",
                    output.toString(), source.toString()).redirectErrorStream(true).start();
        } catch (IOException e) {
            throw new ConversionRequiredException("LibreOffice不可用，旧版DOC需要人工转换为DOCX");
        }
        if (!process.waitFor(60, TimeUnit.SECONDS) || process.exitValue() != 0) {
            process.destroyForcibly();
            throw new ConversionRequiredException("旧版DOC转换失败，请人工转换为DOCX");
        }
        Path converted = output.resolve(stripExtension(source.getFileName().toString()) + ".docx");
        if (!Files.isRegularFile(converted)) throw new ConversionRequiredException("未生成DOCX转换文件");
        return converted;
    }

    private String extractText(Path file) throws Exception {
        String text = documentTextService.extract(file, file.getFileName().toString(), Files.probeContentType(file));
        return text.replace("\u0000", "").replaceAll("[\\t\\r]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
    }

    private boolean isAcceptedPolicyFile(Path root, Path path) {
        Path relative = root.relativize(path);
        for (Path part : relative) {
            String name = part.toString();
            if (name.startsWith(".") || "#recycle".equalsIgnoreCase(name) || name.startsWith("~$")) return false;
        }
        return SUPPORTED.contains(extension(path.getFileName().toString()));
    }

    private KnowledgeImportBatch newBatch(String source, String status, Long userId, int count) {
        KnowledgeImportBatch batch = new KnowledgeImportBatch();
        batch.setSourceType(source); batch.setStatus(status); batch.setCreatedBy(userId); batch.setItemCount(count);
        return batchRepository.save(batch);
    }
    private KnowledgeImportBatch requireBatch(Long id, String source) {
        KnowledgeImportBatch batch = batchRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("导入批次不存在"));
        if (!source.equals(batch.getSourceType())) throw new IllegalArgumentException("导入批次来源不匹配");
        return batch;
    }
    private KnowledgeImportItem requireItem(Long id) { return itemRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("导入项目不存在")); }
    private String resolveBatchStatus(List<KnowledgeImportItem> items) {
        if (items.stream().anyMatch(item -> "PENDING_REVIEW".equals(item.getStatus()))) return "PENDING_REVIEW";
        if (items.stream().anyMatch(item -> "NEEDS_UPLOAD".equals(item.getStatus()))) return "NEEDS_UPLOAD";
        if (items.stream().anyMatch(item -> "STAGED".equals(item.getStatus()))) return "STAGED";
        if (items.stream().anyMatch(item -> "DISCOVERED".equals(item.getStatus()))) return "DISCOVERED";
        if (items.stream().anyMatch(item -> "APPROVED".equals(item.getStatus()))) return "APPROVED";
        if (items.stream().anyMatch(item -> "REJECTED".equals(item.getStatus()))) return "REJECTED";
        return "FAILED";
    }

    private String flkId(String sourceUrl) {
        URI uri = URI.create(sourceUrl);
        if (!FLK_HOST.equalsIgnoreCase(uri.getHost()) || !"/detail".equals(uri.getPath())) return null;
        String query = uri.getRawQuery();
        if (!StringUtils.hasText(query)) return null;
        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');
            String key = separator < 0 ? pair : pair.substring(0, separator);
            String value = separator < 0 ? "" : pair.substring(separator + 1);
            if (("id".equals(key) || "bbbs".equals(key)) && StringUtils.hasText(value)) {
                String decoded = URLDecoder.decode(value, StandardCharsets.UTF_8);
                if (decoded.matches("[A-Za-z0-9-]{20,80}")) return decoded;
                throw new IllegalArgumentException("法规详情链接中的版本标识无效");
            }
        }
        return null;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void requireAllowedOfficialResponse(URI uri, boolean signedDownload) {
        String host = uri == null ? null : uri.getHost();
        boolean allowed = "https".equalsIgnoreCase(uri == null ? null : uri.getScheme())
                && (FLK_HOST.equalsIgnoreCase(host)
                || (signedDownload && host != null && (host.equals("flkoss.obs-bj2.cucloud.cn")
                || host.endsWith(".flk.npc.gov.cn"))));
        if (!allowed) throw new IllegalArgumentException("官方资源跳转到了未允许的地址，已停止下载");
    }

    private String textValue(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText(null);
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private LocalDate dateValue(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String validityStatus(int value) {
        if (value == 3) return "EFFECTIVE";
        if (value == 2) return "AMENDED";
        if (value == 1) return "REPEALED";
        return "UNKNOWN";
    }

    private boolean hasPdfSignature(byte[] content) {
        return content != null && content.length >= 4
                && content[0] == '%' && content[1] == 'P' && content[2] == 'D' && content[3] == 'F';
    }

    private boolean hasZipSignature(byte[] content) {
        return content != null && content.length >= 4
                && content[0] == 'P' && content[1] == 'K'
                && ((content[2] == 3 && content[3] == 4)
                || (content[2] == 5 && content[3] == 6)
                || (content[2] == 7 && content[3] == 8));
    }

    private void validateFlkUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !FLK_HOST.equalsIgnoreCase(uri.getHost())) throw new Exception();
        } catch (Exception e) { throw new IllegalArgumentException("仅允许国家法律法规数据库 https://flk.npc.gov.cn 的链接"); }
    }
    private Path itemStagingPath(KnowledgeImportItem item, String name) {
        Path root = Paths.get(stagingRoot).toAbsolutePath().normalize();
        return root.resolve(String.valueOf(item.getBatchId())).resolve(item.getId() + "_" + safeName(name)).normalize();
    }
    private void requireSupported(String name) { if (!SUPPORTED.contains(extension(name))) throw new IllegalArgumentException("仅支持PDF、DOC、DOCX、TXT、MD"); }
    private String fileNameFromUrl(String url, String type) {
        String path = URI.create(url).getPath(); String name = path == null ? "" : Paths.get(path).getFileName().toString();
        if (SUPPORTED.contains(extension(name))) return safeName(name);
        return type.contains("pdf") ? "official.pdf" : "official.docx";
    }
    private String titleFromUrl(String url) { String path = URI.create(url).getPath(); return StringUtils.hasText(path) ? "国家法律法规数据库条目 " + Paths.get(path).getFileName() : "国家法律法规数据库条目"; }
    private String safeName(String name) { String value = name == null ? "knowledge-document" : Paths.get(name).getFileName().toString(); return value.replaceAll("[\\\\/:*?\"<>|]", "_"); }
    private String extension(String name) { int dot = name.lastIndexOf('.'); return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT); }
    private String stripExtension(String name) { int dot = name.lastIndexOf('.'); return dot > 0 ? name.substring(0, dot) : name; }
    private String sha256(Path path) throws IOException { try { MessageDigest md = MessageDigest.getInstance("SHA-256"); try (InputStream in = Files.newInputStream(path)) { byte[] b = new byte[8192]; int n; while ((n = in.read(b)) > 0) md.update(b, 0, n); } StringBuilder s = new StringBuilder(); for (byte b : md.digest()) s.append(String.format("%02x", b)); return s.toString(); } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }
    private String safeMessage(String value) { if (!StringUtils.hasText(value)) return "处理失败"; return value.length() > 900 ? value.substring(0, 900) : value; }
    private void sleepQuietly(long millis) { try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    private void deleteOnRollback(Path path) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                }
            }
        });
    }

    private void deletePreviousAttachmentAfterCommit(String previousAttachment, Path allowedRoot, Path replacement) {
        if (!StringUtils.hasText(previousAttachment)) return;
        Path previous;
        try {
            previous = Paths.get(previousAttachment).toAbsolutePath().normalize();
        } catch (RuntimeException e) {
            return;
        }
        if (!previous.startsWith(allowedRoot) || previous.equals(replacement)) return;
        afterCommit(() -> {
            try { Files.deleteIfExists(previous); } catch (IOException ignored) { }
        });
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { action.run(); }
        });
    }

    private static class ConversionRequiredException extends Exception { ConversionRequiredException(String message) { super(message); } }
}
