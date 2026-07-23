package com.lawfirm.service;

import com.lawfirm.dto.*;
import com.lawfirm.entity.*;
import com.lawfirm.enums.ArchiveJobStatus;
import com.lawfirm.enums.CaseStatus;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.exception.ResourceNotFoundException;
import com.lawfirm.repository.*;
import com.lawfirm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArchiveWorkflowService {
    private static final String TEMPLATE_VERSION = "CIVIL_V1";
    private static final Set<Integer> CRITICAL_SEQUENCES = Set.of(3, 4);

    private final ArchiveJobRepository archiveJobRepository;
    private final ArchiveDocumentItemRepository archiveDocumentItemRepository;
    private final ArchiveFieldSnapshotRepository archiveFieldSnapshotRepository;
    private final ArchiveOutputRepository archiveOutputRepository;
    private final ArchiveAuditLogRepository archiveAuditLogRepository;
    private final CaseRepository caseRepository;
    private final CaseDocumentRepository caseDocumentRepository;
    private final PartyRepository partyRepository;
    private final UserRepository userRepository;
    private final CaseService caseService;
    private final CaseFileLibraryService caseFileLibraryService;
    private final CaseTimelineService caseTimelineService;
    private final ArchiveWorkerClient archiveWorkerClient;
    private final SecurityUtils securityUtils;
    private final ArchiveTransactionRunner transactionRunner;

    private static final Map<Integer, String> CATALOG = createCatalog();

    @Transactional(readOnly = true)
    public ArchiveReadinessDTO getReadiness(Long caseId, Long userId) {
        Case caseEntity = requireCase(caseId);
        caseService.assertCaseVisible(caseId, userId);
        ArchiveReadinessDTO dto = new ArchiveReadinessDTO();
        dto.setCaseId(caseId);
        dto.setCaseStatus(caseEntity.getStatus());
        dto.setCaseType(caseEntity.getCaseType());
        List<CaseDocument> documents = archivableDocuments(caseId);
        dto.setDocumentCount(documents.size());
        boolean supportedStatus = CaseStatus.CLOSED.getCode().equals(caseEntity.getStatus())
                || CaseStatus.ARCHIVED.getCode().equals(caseEntity.getStatus());
        dto.setCanStart(supportedStatus
                && "CIVIL".equals(caseEntity.getCaseType())
                && canManageCase(caseId, userId));
        if (!supportedStatus) dto.getWarnings().add("案件尚未结案");
        if (CaseStatus.ARCHIVED.getCode().equals(caseEntity.getStatus())) dto.getWarnings().add("再次归档将生成新版本，必须填写更正原因");
        if (!"CIVIL".equals(caseEntity.getCaseType())) dto.getWarnings().add("首期仅支持民事诉讼案件");
        if (documents.isEmpty()) dto.getWarnings().add("案件尚无可归档文件");
        dto.getMissingCritical().addAll(findMissingCritical(caseEntity, documents.stream()
                .map(this::temporaryItem).collect(Collectors.toList()), Collections.emptyMap()));
        dto.setReady(dto.isCanStart() && dto.getMissingCritical().isEmpty());
        return dto;
    }

    @Transactional
    public ArchiveJobDTO createJob(Long caseId, ArchiveJobCreateRequest request, Long userId) {
        Case caseEntity = requireCase(caseId);
        caseService.assertCaseManageable(caseId, userId);
        boolean correction = CaseStatus.ARCHIVED.getCode().equals(caseEntity.getStatus());
        if (!CaseStatus.CLOSED.getCode().equals(caseEntity.getStatus()) && !correction) {
            throw new InvalidParameterException("caseId", "只有已结案案件可以发起归档");
        }
        if (!"CIVIL".equals(caseEntity.getCaseType())) {
            throw new InvalidParameterException("caseType", "首期智能归档仅支持民事诉讼案件");
        }
        if (correction && (request == null || !StringUtils.hasText(request.getCorrectionReason()))) {
            throw new InvalidParameterException("correctionReason", "已归档案件再次归档必须填写更正原因");
        }

        String requestedKey = request == null ? null : request.getIdempotencyKey();
        if (StringUtils.hasText(requestedKey)) {
            Optional<ArchiveJob> existing = archiveJobRepository.findByIdempotencyKeyAndDeletedFalse(requestedKey.trim());
            if (existing.isPresent()) return toDTO(existing.get(), userId);
        }
        Optional<ArchiveJob> active = archiveJobRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId)
                .stream().filter(job -> ArchiveJobStatus.valueOf(job.getStatus()).isActive()).findFirst();
        if (active.isPresent()) return toDTO(active.get(), userId);

        List<CaseDocument> documents = archivableDocuments(caseId);
        if (documents.isEmpty()) throw new InvalidParameterException("documents", "案件没有可归档文件");

        ArchiveJob job = new ArchiveJob();
        job.setCaseId(caseId);
        job.setStatus(ArchiveJobStatus.PRECHECK.name());
        job.setTemplateVersion(TEMPLATE_VERSION);
        job.setIdempotencyKey(StringUtils.hasText(requestedKey) ? requestedKey.trim() : UUID.randomUUID().toString());
        job.setCreatedBy(userId);
        job.setProgress(5);
        job.setCurrentStage("材料预检查");
        job.setCorrectionReason(correction ? request.getCorrectionReason().trim() : null);
        job = archiveJobRepository.save(job);
        audit(job.getId(), userId, correction ? "CORRECTION_CREATE" : "CREATE",
                correction ? "发起归档更正：" + safeAuditText(job.getCorrectionReason()) : "创建民事案件归档任务");

        int order = 0;
        for (CaseDocument document : documents) {
            ArchiveDocumentItem item = temporaryItem(document);
            item.setJobId(job.getId());
            item.setSortOrder(order++);
            archiveDocumentItemRepository.save(item);
        }
        seedFields(job.getId(), caseEntity, userId);
        analyze(job, caseEntity, documents);
        return toDTO(archiveJobRepository.findById(job.getId()).orElseThrow(), userId);
    }

    @Transactional(readOnly = true)
    public ArchiveJobDTO getJob(Long jobId, Long userId) {
        ArchiveJob job = requireJob(jobId);
        assertVisible(job, userId);
        return toDTO(job, userId);
    }

    @Transactional(readOnly = true)
    public List<ArchiveJobDTO> listJobs(String status, Long caseId, Long userId) {
        List<ArchiveJob> jobs;
        if (StringUtils.hasText(status)) {
            jobs = archiveJobRepository.findByStatusAndDeletedFalseOrderByCreatedAtAsc(status.trim().toUpperCase());
        } else if (caseId != null) {
            caseService.assertCaseVisible(caseId, userId);
            jobs = archiveJobRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId);
        } else if (canReview()) {
            jobs = archiveJobRepository.findAll().stream().filter(job -> !Boolean.TRUE.equals(job.getDeleted()))
                    .sorted(Comparator.comparing(ArchiveJob::getCreatedAt).reversed()).collect(Collectors.toList());
        } else {
            jobs = archiveJobRepository.findByCreatedByAndDeletedFalseOrderByCreatedAtDesc(userId);
        }
        return jobs.stream().filter(job -> isVisible(job, userId)).map(job -> toDTO(job, userId)).collect(Collectors.toList());
    }

    @Transactional
    public ArchiveJobDTO patchDocuments(Long jobId, ArchiveDocumentPatchRequest request, Long userId) {
        ArchiveJob job = requireEditableJob(jobId, userId);
        Map<Long, ArchiveDocumentItem> byId = archiveDocumentItemRepository
                .findByJobIdAndDeletedFalseOrderByCatalogSeqAscSortOrderAsc(jobId).stream()
                .collect(Collectors.toMap(ArchiveDocumentItem::getId, item -> item));
        for (ArchiveDocumentPatchRequest.Item patch : request.getItems()) {
            ArchiveDocumentItem item = byId.get(patch.getId());
            if (item == null) throw new InvalidParameterException("documents", "归档文档项不属于当前任务");
            item.setIncluded(patch.getIncluded());
            if (Boolean.TRUE.equals(patch.getIncluded())) {
                if (patch.getCatalogSeq() == null || !CATALOG.containsKey(patch.getCatalogSeq())) {
                    throw new InvalidParameterException("catalogSeq", "请选择有效的民事卷宗目录");
                }
                item.setCatalogSeq(patch.getCatalogSeq());
                item.setCatalogName(StringUtils.hasText(patch.getCatalogName())
                        ? patch.getCatalogName().trim() : CATALOG.get(patch.getCatalogSeq()));
            }
            archiveDocumentItemRepository.save(item);
        }
        audit(jobId, userId, "DOCUMENTS_CONFIRMED", "确认归档材料范围和目录归属");
        return toDTO(job, userId);
    }

    @Transactional
    public ArchiveJobDTO uploadSupplement(Long jobId, MultipartFile file, Integer catalogSeq, Long userId) {
        ArchiveJob job = requireEditableJob(jobId, userId);
        if (file == null || file.isEmpty()) throw new InvalidParameterException("file", "补充材料不能为空");
        if (file.getSize() > 52_428_800L) throw new InvalidParameterException("file", "补充材料不能超过50MB");
        if (catalogSeq == null || !CATALOG.containsKey(catalogSeq)) {
            throw new InvalidParameterException("catalogSeq", "请选择补充材料所属卷内目录");
        }
        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "补充材料";
        String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!Set.of("pdf", "doc", "docx", "xls", "xlsx", "png", "jpg", "jpeg", "tif", "tiff").contains(extension)) {
            throw new InvalidParameterException("file", "仅支持PDF、Word、Excel和图片材料");
        }
        Case caseEntity = requireCase(job.getCaseId());
        Path caseRoot = caseFileLibraryService.ensureCaseFolder(caseEntity).toAbsolutePath().normalize();
        Path inputDir = caseRoot.resolve("99_归档材料").resolve(".archive-input").resolve(String.valueOf(jobId)).normalize();
        ensureInside(caseRoot, inputDir);
        Path stored = inputDir.resolve(System.currentTimeMillis() + "_" + safeFileName(originalName)).normalize();
        ensureInside(caseRoot, stored);
        try {
            Files.createDirectories(inputDir);
            file.transferTo(stored);
        } catch (Exception e) {
            throw new IllegalStateException("补充材料写入失败", e);
        }
        try {
            String sha = sha256(stored);
            if (caseDocumentRepository.existsByCaseIdAndContentSha256AndDeletedFalse(caseEntity.getId(), sha)) {
                throw new InvalidParameterException("file", "该案件已存在内容相同的材料");
            }
            CaseDocument document = new CaseDocument();
            document.setCaseId(caseEntity.getId());
            document.setDocumentName(safeFileName(originalName));
            document.setOriginalFileName(originalName);
            document.setDocumentType("归档补充材料");
            document.setFilePath(stored.toString());
            document.setFileSize(file.getSize());
            document.setMimeType(file.getContentType());
            document.setFolderPath("99_归档材料");
            document.setVersionNo(1);
            document.setUploadBy(userId);
            document.setKnowledgeEligible(false);
            document.setIndexStatus("FORBIDDEN");
            document.setContentSha256(sha);
            document = caseDocumentRepository.save(document);

            ArchiveDocumentItem item = temporaryItem(document);
            item.setJobId(jobId);
            item.setCatalogSeq(catalogSeq);
            item.setCatalogName(CATALOG.get(catalogSeq));
            item.setConfidence(1.0);
            item.setClassificationReason("律师补传时确认目录归属");
            item.setSortOrder(archiveDocumentItemRepository.findByJobIdAndDeletedFalseOrderByCatalogSeqAscSortOrderAsc(jobId).size());
            archiveDocumentItemRepository.save(item);
            audit(jobId, userId, "SUPPLEMENT_UPLOAD", "补传归档材料：" + safeAuditText(originalName));
            return toDTO(job, userId);
        } catch (RuntimeException e) {
            try { Files.deleteIfExists(stored); } catch (Exception ignored) { }
            throw e;
        }
    }

    @Transactional
    public ArchiveJobDTO patchFields(Long jobId, ArchiveFieldsPatchRequest request, Long userId) {
        ArchiveJob job = requireEditableJob(jobId, userId);
        LocalDateTime now = LocalDateTime.now();
        request.getFields().forEach((key, value) -> {
            if (!StringUtils.hasText(key) || key.length() > 100) {
                throw new InvalidParameterException("fields", "归档字段名称不合法");
            }
            if (value != null && value.length() > 10000) {
                throw new InvalidParameterException("fields", key + "内容不能超过10000个字符");
            }
            ArchiveFieldSnapshot field = archiveFieldSnapshotRepository
                    .findByJobIdAndFieldKeyAndDeletedFalse(jobId, key).orElseGet(ArchiveFieldSnapshot::new);
            field.setJobId(jobId);
            field.setFieldKey(key);
            field.setFieldValue(value == null ? "" : value.trim());
            field.setConfirmedBy(userId);
            field.setConfirmedAt(now);
            field.setExtractionReason("律师人工核对");
            archiveFieldSnapshotRepository.save(field);
        });
        audit(jobId, userId, "FIELDS_CONFIRMED", "核对归档表格字段");
        return toDTO(job, userId);
    }

    @Transactional
    public ArchiveJobDTO submit(Long jobId, Long userId) {
        ArchiveJob job = requireEditableJob(jobId, userId);
        List<ArchiveDocumentItem> items = archiveDocumentItemRepository
                .findByJobIdAndDeletedFalseOrderByCatalogSeqAscSortOrderAsc(jobId);
        if (items.stream().noneMatch(item -> Boolean.TRUE.equals(item.getIncluded()))) {
            throw new InvalidParameterException("documents", "至少保留一份归档材料");
        }
        if (items.stream().anyMatch(item -> Boolean.TRUE.equals(item.getIncluded())
                && (item.getCatalogSeq() == null || !CATALOG.containsKey(item.getCatalogSeq())))) {
            throw new InvalidParameterException("documents", "仍有材料未完成目录归类");
        }
        job.setStatus(ArchiveJobStatus.ADMIN_REVIEW.name());
        job.setSubmittedBy(userId);
        job.setSubmittedAt(LocalDateTime.now());
        job.setProgress(70);
        job.setCurrentStage("等待行政复核");
        archiveJobRepository.save(job);
        audit(jobId, userId, "SUBMIT", "律师提交行政归档复核");
        return toDTO(job, userId);
    }

    public ArchiveJobDTO review(Long jobId, ArchiveReviewRequest request, Long userId) {
        if (!canReview()) throw new AccessDeniedException("无归档复核权限");
        String decision = request.getDecision().trim().toUpperCase();
        if ("REJECT".equals(decision)) {
            if (!StringUtils.hasText(request.getReason())) {
                throw new InvalidParameterException("reason", "驳回归档必须填写理由");
            }
            return transactionRunner.execute(() -> rejectReview(jobId, request, userId));
        }
        if (!"APPROVE".equals(decision)) {
            throw new InvalidParameterException("decision", "复核决定仅支持 APPROVE 或 REJECT");
        }

        transactionRunner.execute(() -> prepareAssembly(jobId, request, userId));
        try {
            transactionRunner.execute(() -> {
                ArchiveJob job = requireJobForUpdate(jobId);
                if (!ArchiveJobStatus.ASSEMBLING.name().equals(job.getStatus())) {
                    throw new InvalidParameterException("status", "归档任务当前不在电子卷宗生成阶段");
                }
                Case caseEntity = requireCase(job.getCaseId());
                assembleAndComplete(job, caseEntity, fieldMap(jobId), userId);
                return null;
            });
        } catch (RuntimeException e) {
            transactionRunner.execute(() -> {
                markAssemblyFailed(jobId, userId, e);
                return null;
            });
        }
        return transactionRunner.execute(() -> toDTO(requireJob(jobId), userId));
    }

    private ArchiveJobDTO rejectReview(Long jobId, ArchiveReviewRequest request, Long userId) {
        ArchiveJob job = requireJobForUpdate(jobId);
        requireAdministrativeReview(job);
        job.setStatus(ArchiveJobStatus.REJECTED.name());
        job.setReviewReason(request.getReason().trim());
        job.setReviewedBy(userId);
        job.setReviewedAt(LocalDateTime.now());
        job.setCurrentStage("行政已驳回");
        archiveJobRepository.save(job);
        audit(jobId, userId, "REJECT", "行政驳回归档：" + safeAuditText(request.getReason()));
        return toDTO(job, userId);
    }

    private Void prepareAssembly(Long jobId, ArchiveReviewRequest request, Long userId) {
        ArchiveJob job = requireJobForUpdate(jobId);
        requireAdministrativeReview(job);

        Case caseEntity = requireCase(job.getCaseId());
        Map<String, String> fields = fieldMap(jobId);
        List<String> missing = findMissingCritical(caseEntity,
                archiveDocumentItemRepository.findByJobIdAndDeletedFalseOrderByCatalogSeqAscSortOrderAsc(jobId), fields);
        if (!missing.isEmpty() && !StringUtils.hasText(request.getExceptionReason())) {
            throw new InvalidParameterException("exceptionReason", "关键材料缺失，行政放行必须填写例外理由：" + String.join("、", missing));
        }
        job.setExceptionReason(StringUtils.hasText(request.getExceptionReason()) ? request.getExceptionReason().trim() : null);
        job.setReviewedBy(userId);
        job.setReviewedAt(LocalDateTime.now());
        job.setReviewReason(StringUtils.hasText(request.getReason()) ? request.getReason().trim() : "行政复核通过");
        job.setStatus(ArchiveJobStatus.ASSEMBLING.name());
        job.setProgress(80);
        job.setCurrentStage("生成电子卷宗");
        archiveJobRepository.save(job);
        audit(jobId, userId, "APPROVE", missing.isEmpty() ? "行政复核通过" : "行政例外放行：" + safeAuditText(job.getExceptionReason()));
        return null;
    }

    private void markAssemblyFailed(Long jobId, Long userId, RuntimeException error) {
        ArchiveJob job = requireJobForUpdate(jobId);
        if (ArchiveJobStatus.ASSEMBLING.name().equals(job.getStatus())) {
            job.setStatus(ArchiveJobStatus.FAILED.name());
            job.setErrorMessage(safeError(error));
            job.setCurrentStage("电子卷宗生成失败");
            archiveJobRepository.save(job);
            audit(jobId, userId, "ASSEMBLE_FAILED", safeError(error));
        }
    }

    private void requireAdministrativeReview(ArchiveJob job) {
        if (!ArchiveJobStatus.ADMIN_REVIEW.name().equals(job.getStatus())) {
            throw new InvalidParameterException("status", "归档任务当前不在行政复核阶段");
        }
    }

    @Transactional(readOnly = true)
    public Path getOutputPath(Long jobId, Long userId) {
        ArchiveJob job = requireJob(jobId);
        assertVisible(job, userId);
        ArchiveOutput output = archiveOutputRepository.findFirstByJobIdAndDeletedFalseOrderByVersionNoDesc(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("归档电子卷宗", jobId));
        Path path = Paths.get(output.getFilePath()).toAbsolutePath().normalize();
        Path root = caseFileLibraryService.getCaseLibraryRootPath().toAbsolutePath().normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new ResourceNotFoundException("归档电子卷宗", jobId);
        }
        return path;
    }

    @Transactional
    public Path getPreviewPath(Long jobId, Long userId) {
        ArchiveJob job = requireJob(jobId);
        assertVisible(job, userId);
        if (ArchiveJobStatus.COMPLETED.name().equals(job.getStatus())) {
            return getOutputPath(jobId, userId);
        }
        if (!Set.of(ArchiveJobStatus.LAWYER_REVIEW.name(), ArchiveJobStatus.ADMIN_REVIEW.name(),
                ArchiveJobStatus.REJECTED.name()).contains(job.getStatus())) {
            throw new InvalidParameterException("status", "归档任务当前不能生成预览");
        }
        Case caseEntity = requireCase(job.getCaseId());
        Path caseRoot = caseFileLibraryService.ensureCaseFolder(caseEntity).toAbsolutePath().normalize();
        Path previewDir = caseRoot.resolve("99_归档材料").resolve(".archive-preview").normalize();
        Path preview = previewDir.resolve("archive-preview-" + jobId + ".pdf").normalize();
        ensureInside(caseRoot, preview);
        try {
            Files.createDirectories(previewDir);
            Files.deleteIfExists(preview);
            Files.deleteIfExists(manifestPath(preview));
        } catch (Exception e) {
            throw new IllegalStateException("无法准备归档预览目录", e);
        }
        try {
            renderArchiveArtifact(job, caseEntity, fieldMap(jobId), preview, false);
        } catch (RuntimeException e) {
            deleteArtifact(preview);
            throw e;
        }
        audit(jobId, userId, "PREVIEW", "生成电子卷宗预览");
        return preview;
    }

    private void analyze(ArchiveJob job, Case caseEntity, List<CaseDocument> documents) {
        job.setStatus(ArchiveJobStatus.OCR.name());
        job.setProgress(15);
        job.setCurrentStage("本地OCR与文字提取");
        archiveJobRepository.save(job);
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("jobId", job.getId());
            payload.put("caseId", caseEntity.getId());
            payload.put("templateVersion", TEMPLATE_VERSION);
            payload.put("caseMetadata", fieldMap(job.getId()));
            List<Map<String, Object>> docs = new ArrayList<>();
            Path caseRoot = caseFileLibraryService.ensureCaseFolder(caseEntity).toAbsolutePath().normalize();
            for (CaseDocument document : documents) {
                Path path = Paths.get(document.getFilePath()).toAbsolutePath().normalize();
                if (!path.startsWith(caseRoot) || !Files.isRegularFile(path)) continue;
                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("caseDocumentId", document.getId());
                doc.put("fileName", document.getOriginalFileName());
                doc.put("path", path.toString());
                doc.put("documentType", document.getDocumentType());
                doc.put("contentSha256", document.getContentSha256());
                docs.add(doc);
            }
            payload.put("documents", docs);
            job.setStatus(ArchiveJobStatus.CLASSIFYING.name());
            job.setProgress(30);
            job.setCurrentStage("材料分类与目录排序");
            archiveJobRepository.save(job);
            Map<String, Object> result = archiveWorkerClient.analyze(payload);
            mergeAnalysis(job.getId(), result);
            job.setStatus(ArchiveJobStatus.LAWYER_REVIEW.name());
            job.setProgress(60);
            job.setCurrentStage("等待律师核对");
            job.setErrorMessage(null);
            Object modelName = result.get("modelName");
            if (modelName != null) job.setModelName(String.valueOf(modelName));
            archiveJobRepository.save(job);
            audit(job.getId(), job.getCreatedBy(), "ANALYSIS_COMPLETE", "本地OCR、分类和字段提取完成");
        } catch (RuntimeException e) {
            job.setStatus(ArchiveJobStatus.FAILED.name());
            job.setProgress(0);
            job.setCurrentStage("本地归档分析失败");
            job.setErrorMessage(safeError(e));
            archiveJobRepository.save(job);
            audit(job.getId(), job.getCreatedBy(), "ANALYSIS_FAILED", safeError(e));
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeAnalysis(Long jobId, Map<String, Object> result) {
        Map<Long, ArchiveDocumentItem> items = archiveDocumentItemRepository
                .findByJobIdAndDeletedFalseOrderByCatalogSeqAscSortOrderAsc(jobId).stream()
                .collect(Collectors.toMap(ArchiveDocumentItem::getCaseDocumentId, item -> item));
        Object documentValue = result.get("documents");
        if (documentValue instanceof List) {
            for (Object value : (List<?>) documentValue) {
                if (!(value instanceof Map)) continue;
                Map<String, Object> doc = (Map<String, Object>) value;
                Long documentId = longValue(doc.get("caseDocumentId"));
                ArchiveDocumentItem item = items.get(documentId);
                if (item == null) continue;
                Integer seq = intValue(doc.get("catalogSeq"));
                if (seq != null && CATALOG.containsKey(seq)) {
                    item.setCatalogSeq(seq);
                    item.setCatalogName(CATALOG.get(seq));
                }
                item.setDocumentType(stringValue(doc.get("documentType"), item.getDocumentType()));
                item.setSourcePageCount(defaultInt(doc.get("sourcePageCount"), item.getSourcePageCount()));
                item.setConfidence(doubleValue(doc.get("confidence")));
                item.setClassificationReason(limit(stringValue(doc.get("reason"), "本地归档引擎识别"), 1000));
                archiveDocumentItemRepository.save(item);
            }
        }
        Object fieldValue = result.get("fields");
        if (fieldValue instanceof List) {
            for (Object value : (List<?>) fieldValue) {
                if (!(value instanceof Map)) continue;
                Map<String, Object> fieldData = (Map<String, Object>) value;
                String key = stringValue(fieldData.get("key"), "");
                if (!StringUtils.hasText(key) || key.length() > 100) continue;
                ArchiveFieldSnapshot field = archiveFieldSnapshotRepository
                        .findByJobIdAndFieldKeyAndDeletedFalse(jobId, key).orElseGet(ArchiveFieldSnapshot::new);
                field.setJobId(jobId);
                field.setFieldKey(key);
                String extracted = stringValue(fieldData.get("value"), "");
                if (StringUtils.hasText(extracted) && !"待确认".equals(extracted)) field.setFieldValue(extracted);
                field.setSourceDocumentId(longValue(fieldData.get("sourceDocumentId")));
                field.setSourcePage(intValue(fieldData.get("sourcePage")));
                field.setConfidence(doubleValue(fieldData.get("confidence")));
                field.setExtractionReason(limit(stringValue(fieldData.get("reason"), "本地模型提取"), 1000));
                archiveFieldSnapshotRepository.save(field);
            }
        }
    }

    private void assembleAndComplete(ArchiveJob job, Case caseEntity, Map<String, String> fields, Long userId) {
        Path caseRoot = caseFileLibraryService.ensureCaseFolder(caseEntity).toAbsolutePath().normalize();
        Path archiveDir = caseRoot.resolve("99_归档材料").normalize();
        ensureInside(caseRoot, archiveDir);
        try { Files.createDirectories(archiveDir); } catch (Exception e) { throw new IllegalStateException("无法创建归档目录", e); }
        int version = archiveOutputRepository.findByCaseIdAndDeletedFalseOrderByVersionNoDesc(caseEntity.getId())
                .stream().map(ArchiveOutput::getVersionNo).max(Integer::compareTo).orElse(0) + 1;
        String caseNo = safeFileName(StringUtils.hasText(caseEntity.getCaseNumber()) ? caseEntity.getCaseNumber() : "CASE_" + caseEntity.getId());
        String fileName = caseNo + "_电子卷宗_v" + version + ".pdf";
        Path staging = archiveDir.resolve("." + caseNo + "_电子卷宗_v" + version + ".staging-" + job.getId() + ".pdf").normalize();
        Path target = archiveDir.resolve(fileName).normalize();
        Path manifestTarget = archiveDir.resolve(fileName + ".manifest.json").normalize();
        ensureInside(caseRoot, staging);
        ensureInside(caseRoot, target);
        ensureInside(caseRoot, manifestTarget);
        if (Files.exists(target)) throw new IllegalStateException("归档版本文件已存在，禁止覆盖");
        if (Files.exists(manifestTarget)) throw new IllegalStateException("归档版本清单已存在，禁止覆盖");

        try {
            Map<String, Object> result = renderArchiveArtifact(job, caseEntity, fields, staging, true);
            int gaps = defaultInt(result.get("gapPages"), -1);
            int duplicates = defaultInt(result.get("duplicatePages"), -1);
            String sha = sha256(staging);
            String workerSha = stringValue(result.get("sha256"), "");
            if (StringUtils.hasText(workerSha) && !sha.equalsIgnoreCase(workerSha)) {
                throw new IllegalStateException("归档文件哈希校验失败");
            }
            Path stagingManifest = manifestPath(staging);
            if (!Files.isRegularFile(stagingManifest)) {
                throw new IllegalStateException("归档Worker未生成来源清单");
            }
            String manifestSha = sha256(stagingManifest);
            try {
                Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(staging, target);
            }
            try {
                try {
                    Files.move(stagingManifest, manifestTarget, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(stagingManifest, manifestTarget);
                }
            } catch (Exception e) {
                throw new IllegalStateException("归档来源清单写入失败", e);
            }

            ArchiveOutput output = new ArchiveOutput();
            output.setJobId(job.getId());
            output.setCaseId(caseEntity.getId());
            output.setVersionNo(version);
            output.setFileName(fileName);
            output.setFilePath(target.toString());
            output.setContentSha256(sha);
            output.setManifestFilePath(manifestTarget.toString());
            output.setManifestSha256(manifestSha);
            output.setPageCount(defaultInt(result.get("pageCount"), 0));
            output.setSourcePageCount(defaultInt(result.get("sourcePageCount"), 0));
            output.setGapPages(gaps);
            output.setDuplicatePages(duplicates);
            output.setTemplateVersion(TEMPLATE_VERSION);
            output.setCreatedBy(userId);
            archiveOutputRepository.save(output);

            registerGeneratedDocument(caseEntity, userId, version, fileName, target, "电子卷宗", "application/pdf", sha);
            registerGeneratedDocument(caseEntity, userId, version, manifestTarget.getFileName().toString(), manifestTarget,
                    "电子卷宗归档清单", "application/json", manifestSha);

            caseEntity.setStatus(CaseStatus.ARCHIVED.getCode());
            caseEntity.setArchiveDate(LocalDate.now());
            caseEntity.setArchiveLocation(archiveDir.toString());
            caseRepository.save(caseEntity);
            job.setStatus(ArchiveJobStatus.COMPLETED.name());
            job.setProgress(100);
            job.setCurrentStage("电子卷宗已生成并锁定");
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage(null);
            archiveJobRepository.save(job);
            caseTimelineService.createSystemTimeline(caseEntity.getId(), "CASE_ARCHIVED",
                    "行政复核通过，已生成电子卷宗 " + fileName);
            audit(job.getId(), userId, "COMPLETE", "电子卷宗生成完成，SHA-256=" + sha);
        } catch (Exception e) {
            deleteArtifact(staging);
            deleteArtifact(target);
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new IllegalStateException("归档文件写入失败", e);
        }
    }

    private ArchiveJob requireJobForUpdate(Long jobId) {
        return archiveJobRepository.findActiveByIdForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("归档任务", jobId));
    }

    private Map<String, Object> renderArchiveArtifact(ArchiveJob job, Case caseEntity, Map<String, String> fields,
                                                       Path outputPath, boolean persistRanges) {
        List<ArchiveDocumentItem> items = archiveDocumentItemRepository
                .findByJobIdAndDeletedFalseOrderByCatalogSeqAscSortOrderAsc(job.getId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getIncluded()))
                .sorted(Comparator.comparing(ArchiveDocumentItem::getCatalogSeq).thenComparing(ArchiveDocumentItem::getSortOrder))
                .collect(Collectors.toList());
        if (items.isEmpty()) throw new InvalidParameterException("documents", "至少保留一份归档材料");
        Map<Long, CaseDocument> documents = caseDocumentRepository.findAllById(items.stream()
                .map(ArchiveDocumentItem::getCaseDocumentId).collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(CaseDocument::getId, document -> document));
        Path caseRoot = caseFileLibraryService.ensureCaseFolder(caseEntity).toAbsolutePath().normalize();
        ensureInside(caseRoot, outputPath);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobId", job.getId());
        payload.put("templateVersion", TEMPLATE_VERSION);
        payload.put("caseNumber", caseEntity.getCaseNumber());
        payload.put("caseName", caseEntity.getCaseName());
        payload.put("fields", fields);
        payload.put("exceptionReason", job.getExceptionReason());
        payload.put("correctionReason", job.getCorrectionReason());
        payload.put("outputPath", outputPath.toString());
        List<Map<String, Object>> workerDocs = new ArrayList<>();
        for (ArchiveDocumentItem item : items) {
            CaseDocument document = documents.get(item.getCaseDocumentId());
            if (document == null) throw new IllegalStateException("归档材料记录已不存在");
            Path source = Paths.get(document.getFilePath()).toAbsolutePath().normalize();
            ensureInside(caseRoot, source);
            if (!Files.isRegularFile(source)) throw new IllegalStateException("归档源材料无法读取：" + item.getOriginalFileName());
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("itemId", item.getId());
            value.put("caseDocumentId", document.getId());
            value.put("fileName", document.getOriginalFileName());
            value.put("path", source.toString());
            value.put("catalogSeq", item.getCatalogSeq());
            value.put("catalogName", item.getCatalogName());
            value.put("contentSha256", item.getContentSha256());
            workerDocs.add(value);
        }
        payload.put("documents", workerDocs);
        Map<String, Object> result = archiveWorkerClient.assemble(payload);
        if (!Boolean.TRUE.equals(result.get("success")) || !Files.isRegularFile(outputPath)) {
            throw new IllegalStateException("归档Worker未生成有效电子卷宗");
        }
        int gaps = defaultInt(result.get("gapPages"), -1);
        int duplicates = defaultInt(result.get("duplicatePages"), -1);
        if (gaps != 0 || duplicates != 0) throw new IllegalStateException("源页面守恒校验失败");
        if (persistRanges) applyDocumentRanges(job.getId(), result.get("documentRanges"));
        return result;
    }

    @SuppressWarnings("unchecked")
    private void applyDocumentRanges(Long jobId, Object rangeValue) {
        if (!(rangeValue instanceof List)) return;
        Map<Long, ArchiveDocumentItem> items = archiveDocumentItemRepository
                .findByJobIdAndDeletedFalseOrderByCatalogSeqAscSortOrderAsc(jobId).stream()
                .collect(Collectors.toMap(ArchiveDocumentItem::getId, item -> item));
        for (Object value : (List<?>) rangeValue) {
            if (!(value instanceof Map)) continue;
            Map<String, Object> range = (Map<String, Object>) value;
            ArchiveDocumentItem item = items.get(longValue(range.get("itemId")));
            if (item == null) continue;
            item.setOutputStartPage(intValue(range.get("startPage")));
            item.setOutputEndPage(intValue(range.get("endPage")));
            archiveDocumentItemRepository.save(item);
        }
    }

    private void registerGeneratedDocument(Case caseEntity, Long userId, int version, String fileName, Path path,
                                           String documentType, String mimeType, String sha) {
        CaseDocument generated = new CaseDocument();
        generated.setCaseId(caseEntity.getId());
        generated.setDocumentName(fileName);
        generated.setOriginalFileName(fileName);
        generated.setDocumentType(documentType);
        generated.setFilePath(path.toString());
        try { generated.setFileSize(Files.size(path)); } catch (Exception ignored) { generated.setFileSize(0L); }
        generated.setMimeType(mimeType);
        generated.setFolderPath("99_归档材料");
        generated.setVersionNo(version);
        generated.setUploadBy(userId);
        generated.setKnowledgeEligible(false);
        generated.setIndexStatus("FORBIDDEN");
        generated.setContentSha256(sha);
        caseDocumentRepository.save(generated);
    }

    private Path manifestPath(Path pdfPath) {
        return pdfPath.resolveSibling(pdfPath.getFileName().toString() + ".manifest.json").normalize();
    }

    private void deleteArtifact(Path pdfPath) {
        try { Files.deleteIfExists(pdfPath); } catch (Exception ignored) { }
        try { Files.deleteIfExists(manifestPath(pdfPath)); } catch (Exception ignored) { }
    }

    private ArchiveDocumentItem temporaryItem(CaseDocument document) {
        ArchiveDocumentItem item = new ArchiveDocumentItem();
        item.setCaseDocumentId(document.getId());
        item.setOriginalFileName(StringUtils.hasText(document.getOriginalFileName())
                ? document.getOriginalFileName() : document.getDocumentName());
        int seq = classify(document);
        item.setCatalogSeq(CATALOG.containsKey(seq) ? seq : null);
        item.setCatalogName(CATALOG.getOrDefault(seq, "待人工归类"));
        item.setDocumentType(document.getDocumentType());
        item.setIncluded(true);
        item.setContentSha256(document.getContentSha256());
        item.setConfidence(CATALOG.containsKey(seq) ? 0.72 : 0.0);
        item.setClassificationReason(CATALOG.containsKey(seq) ? "根据文件名和现有文档类型预分类" : "无法可靠识别，须人工归类");
        return item;
    }

    private List<CaseDocument> archivableDocuments(Long caseId) {
        return caseDocumentRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId).stream()
                .filter(document -> !"电子卷宗".equals(document.getDocumentType()))
                .filter(document -> !"电子卷宗归档清单".equals(document.getDocumentType()))
                .collect(Collectors.toList());
    }

    private int classify(CaseDocument document) {
        String text = ((document.getOriginalFileName() == null ? "" : document.getOriginalFileName()) + " "
                + (document.getDocumentName() == null ? "" : document.getDocumentName()) + " "
                + (document.getDocumentType() == null ? "" : document.getDocumentType())).toLowerCase();
        if (contains(text, "发票", "收费", "付款", "收款")) return 2;
        if (contains(text, "委托代理合同", "法律服务合同", "代理合同")) return 3;
        if (contains(text, "授权委托书", "授权书")) return 4;
        if (contains(text, "起诉状", "答辩状", "上诉状", "反诉状")) return 5;
        if (contains(text, "谈话笔录", "阅卷笔录", "会见笔录")) return 6;
        if (contains(text, "证据", "合同", "流水", "借据", "对账单")) return 7;
        if (contains(text, "保全", "查封", "冻结")) return 8;
        if (contains(text, "代理意见", "法律意见")) return 9;
        if (contains(text, "集体讨论", "讨论记录")) return 10;
        if (contains(text, "代理词", "辩护词")) return 11;
        if (contains(text, "传票", "出庭通知")) return 12;
        if (contains(text, "庭审笔录", "开庭笔录")) return 13;
        if (contains(text, "执行", "限制消费", "终本")) return 15;
        if (contains(text, "判决书", "裁定书", "调解书", "裁判文书")) return 14;
        return 99;
    }

    private void seedFields(Long jobId, Case caseEntity, Long userId) {
        List<Party> parties = partyRepository.findByCaseIdAndDeletedFalse(caseEntity.getId());
        String clients = joinPartyNames(parties.stream().filter(p -> Boolean.TRUE.equals(p.getIsClient())).collect(Collectors.toList()));
        String opponents = joinPartyNames(parties.stream().filter(p -> !Boolean.TRUE.equals(p.getIsClient())).collect(Collectors.toList()));
        String owner = userRepository.findById(caseEntity.getOwnerId()).map(User::getRealName).orElse("");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("案件类别", "民事诉讼");
        fields.put("合同号", value(caseEntity.getCaseNumber()));
        fields.put("承办律师", owner);
        fields.put("委托人", clients);
        fields.put("当事人", clients);
        fields.put("对方当事人", opponents);
        fields.put("案由", value(caseEntity.getCaseReason()));
        fields.put("收案日期", date(caseEntity.getFilingDate()));
        fields.put("结案日期", date(caseEntity.getCloseDate()));
        fields.put("审理法院", value(caseEntity.getCourt()));
        fields.put("法院收案号", value(caseEntity.getCourtCaseNumber()));
        fields.put("收费标准", feeSummary(caseEntity));
        fields.put("案情简介", value(caseEntity.getSummary()));
        fields.put("结案小结", "");
        fields.put("归档日期", LocalDate.now().toString());
        fields.put("立卷人", userRepository.findById(userId).map(User::getRealName).orElse(""));
        fields.put("案件或项目名称", value(caseEntity.getCaseName()));
        fields.put("应收业务费", caseEntity.getAttorneyFee() == null ? "" : caseEntity.getAttorneyFee().toPlainString());
        fields.put("已收业务费", caseEntity.getActualReceived() == null ? "" : caseEntity.getActualReceived().toPlainString());
        fields.forEach((key, fieldValue) -> saveSeedField(jobId, key, fieldValue));
    }

    private void saveSeedField(Long jobId, String key, String value) {
        ArchiveFieldSnapshot field = new ArchiveFieldSnapshot();
        field.setJobId(jobId);
        field.setFieldKey(key);
        field.setFieldValue(value);
        field.setConfidence(StringUtils.hasText(value) ? 1.0 : 0.0);
        field.setExtractionReason(StringUtils.hasText(value) ? "来自ZGAI案件结构化数据" : "待本地模型提取或律师确认");
        archiveFieldSnapshotRepository.save(field);
    }

    private List<String> findMissingCritical(Case caseEntity, List<ArchiveDocumentItem> items, Map<String, String> fields) {
        List<String> missing = new ArrayList<>();
        Set<Integer> includedSeq = items.stream().filter(item -> Boolean.TRUE.equals(item.getIncluded()))
                .map(ArchiveDocumentItem::getCatalogSeq).filter(Objects::nonNull).collect(Collectors.toSet());
        if (!includedSeq.contains(3)) missing.add("委托代理合同");
        if (!includedSeq.contains(4)) missing.add("授权委托书");
        boolean hasOutcomeDocument = includedSeq.contains(14) || includedSeq.contains(15);
        if (!hasOutcomeDocument && !StringUtils.hasText(fields.get("结案小结"))) missing.add("案件结案依据");
        if (caseEntity.getFilingDate() == null) missing.add("立案审批记录/立案日期");
        if (caseEntity.getCloseDate() == null && !StringUtils.hasText(fields.get("结案日期"))) missing.add("结案日期");
        return missing;
    }

    private ArchiveJob requireEditableJob(Long jobId, Long userId) {
        ArchiveJob job = requireJob(jobId);
        assertVisible(job, userId);
        if (!Set.of(ArchiveJobStatus.LAWYER_REVIEW.name(), ArchiveJobStatus.REJECTED.name()).contains(job.getStatus())) {
            throw new InvalidParameterException("status", "归档任务当前不能修改");
        }
        if (!Objects.equals(job.getCreatedBy(), userId) && !securityUtils.isAdmin()) {
            throw new AccessDeniedException("只有发起律师可以核对归档任务");
        }
        caseService.assertCaseManageable(job.getCaseId(), userId);
        return job;
    }

    private boolean canManageCase(Long caseId, Long userId) {
        try { caseService.assertCaseManageable(caseId, userId); return true; }
        catch (RuntimeException e) { return false; }
    }

    private boolean canReview() {
        return securityUtils.hasAuthority("CASE_ARCHIVE_REVIEW") || securityUtils.isAdmin();
    }

    private void assertVisible(ArchiveJob job, Long userId) {
        if (!isVisible(job, userId)) throw new AccessDeniedException("无权查看该归档任务");
    }

    private boolean isVisible(ArchiveJob job, Long userId) {
        try { caseService.assertCaseVisible(job.getCaseId(), userId); return true; }
        catch (RuntimeException e) { return canReview(); }
    }

    private Case requireCase(Long caseId) {
        return caseRepository.findById(caseId).filter(value -> !Boolean.TRUE.equals(value.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("案件", caseId));
    }

    private ArchiveJob requireJob(Long jobId) {
        return archiveJobRepository.findById(jobId).filter(value -> !Boolean.TRUE.equals(value.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("归档任务", jobId));
    }

    private ArchiveJobDTO toDTO(ArchiveJob job, Long userId) {
        ArchiveJobDTO dto = new ArchiveJobDTO();
        dto.setId(job.getId());
        dto.setCaseId(job.getCaseId());
        caseRepository.findById(job.getCaseId()).ifPresent(value -> {
            dto.setCaseName(value.getCaseName());
            dto.setCaseNumber(value.getCaseNumber());
        });
        dto.setStatus(job.getStatus());
        dto.setTemplateVersion(job.getTemplateVersion());
        dto.setProgress(job.getProgress());
        dto.setCurrentStage(job.getCurrentStage());
        dto.setErrorMessage(job.getErrorMessage());
        dto.setReviewReason(job.getReviewReason());
        dto.setExceptionReason(job.getExceptionReason());
        dto.setCorrectionReason(job.getCorrectionReason());
        dto.setCreatedBy(job.getCreatedBy());
        dto.setCreatedByName(userName(job.getCreatedBy()));
        dto.setReviewedBy(job.getReviewedBy());
        dto.setReviewedByName(userName(job.getReviewedBy()));
        dto.setCreatedAt(job.getCreatedAt());
        dto.setSubmittedAt(job.getSubmittedAt());
        dto.setReviewedAt(job.getReviewedAt());
        dto.setCompletedAt(job.getCompletedAt());
        boolean editable = Set.of(ArchiveJobStatus.LAWYER_REVIEW.name(), ArchiveJobStatus.REJECTED.name()).contains(job.getStatus())
                && (Objects.equals(job.getCreatedBy(), userId) || securityUtils.isAdmin());
        dto.setCanEdit(editable);
        dto.setCanSubmit(editable);
        dto.setCanReview(ArchiveJobStatus.ADMIN_REVIEW.name().equals(job.getStatus()) && canReview());
        dto.setCanDownload(ArchiveJobStatus.COMPLETED.name().equals(job.getStatus()));
        List<ArchiveDocumentItem> items = archiveDocumentItemRepository
                .findByJobIdAndDeletedFalseOrderByCatalogSeqAscSortOrderAsc(job.getId());
        for (ArchiveDocumentItem item : items) {
            ArchiveJobDTO.DocumentItem value = new ArchiveJobDTO.DocumentItem();
            value.setId(item.getId()); value.setCaseDocumentId(item.getCaseDocumentId());
            value.setOriginalFileName(item.getOriginalFileName()); value.setCatalogSeq(item.getCatalogSeq());
            value.setCatalogName(item.getCatalogName()); value.setDocumentType(item.getDocumentType());
            value.setIncluded(item.getIncluded()); value.setSourcePageCount(item.getSourcePageCount());
            value.setOutputStartPage(item.getOutputStartPage()); value.setOutputEndPage(item.getOutputEndPage());
            value.setContentSha256(item.getContentSha256()); value.setConfidence(item.getConfidence());
            value.setClassificationReason(item.getClassificationReason());
            dto.getDocuments().add(value);
        }
        for (ArchiveFieldSnapshot field : archiveFieldSnapshotRepository.findByJobIdAndDeletedFalseOrderByIdAsc(job.getId())) {
            ArchiveJobDTO.FieldItem value = new ArchiveJobDTO.FieldItem();
            value.setKey(field.getFieldKey()); value.setValue(field.getFieldValue());
            value.setSourceDocumentId(field.getSourceDocumentId()); value.setSourcePage(field.getSourcePage());
            value.setConfidence(field.getConfidence()); value.setExtractionReason(field.getExtractionReason());
            value.setConfirmed(field.getConfirmedBy() != null);
            dto.getFields().add(value);
        }
        dto.getMissingCritical().addAll(findMissingCritical(requireCase(job.getCaseId()), items, fieldMap(job.getId())));
        archiveOutputRepository.findFirstByJobIdAndDeletedFalseOrderByVersionNoDesc(job.getId()).ifPresent(output -> {
            ArchiveJobDTO.OutputItem value = new ArchiveJobDTO.OutputItem();
            value.setId(output.getId()); value.setVersionNo(output.getVersionNo()); value.setFileName(output.getFileName());
            value.setContentSha256(output.getContentSha256()); value.setManifestSha256(output.getManifestSha256());
            value.setPageCount(output.getPageCount());
            value.setSourcePageCount(output.getSourcePageCount()); value.setGapPages(output.getGapPages());
            value.setDuplicatePages(output.getDuplicatePages()); dto.setOutput(value);
        });
        return dto;
    }

    private Map<String, String> fieldMap(Long jobId) {
        return archiveFieldSnapshotRepository.findByJobIdAndDeletedFalseOrderByIdAsc(jobId).stream()
                .collect(Collectors.toMap(ArchiveFieldSnapshot::getFieldKey,
                        field -> value(field.getFieldValue()), (left, right) -> right, LinkedHashMap::new));
    }

    private void audit(Long jobId, Long userId, String action, String detail) {
        ArchiveAuditLog log = new ArchiveAuditLog();
        log.setJobId(jobId); log.setOperatorId(userId); log.setAction(action); log.setDetail(limit(detail, 2000));
        archiveAuditLogRepository.save(log);
    }

    private String userName(Long id) { return id == null ? null : userRepository.findById(id).map(User::getRealName).orElse(null); }
    private String joinPartyNames(List<Party> parties) { return parties.stream().map(Party::getName).filter(StringUtils::hasText).distinct().collect(Collectors.joining("、")); }
    private String date(LocalDate value) { return value == null ? "" : value.toString(); }
    private String value(String value) { return value == null ? "" : value; }
    private String feeSummary(Case c) {
        if ("CONTINGENT".equals(c.getFeeMethod())) return "风险收费 " + (c.getRiskRatio() == null ? "" : c.getRiskRatio().stripTrailingZeros().toPlainString() + "%");
        if ("BASE_PLUS_CONTINGENT".equals(c.getFeeMethod()) || "FIXED_PLUS_CONTINGENT".equals(c.getFeeMethod())) {
            return "固定收费" + money(c.getAttorneyFee()) + "+风险收费" + (c.getRiskRatio() == null ? "" : c.getRiskRatio().stripTrailingZeros().toPlainString() + "%");
        }
        if ("OTHER".equals(c.getFeeMethod())) return StringUtils.hasText(c.getFeeNotes()) ? c.getFeeNotes() : "其他";
        return "固定收费" + money(c.getAttorneyFee());
    }
    private String money(java.math.BigDecimal value) { return value == null ? "" : value.stripTrailingZeros().toPlainString() + "元"; }
    private boolean contains(String value, String... keys) { for (String key : keys) if (value.contains(key)) return true; return false; }
    private String safeAuditText(String value) { return limit(value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim(), 300); }
    private String safeError(Throwable error) { return limit(error.getMessage() == null ? "归档处理失败" : error.getMessage(), 500); }
    private String limit(String value, int max) { return value == null ? null : value.substring(0, Math.min(max, value.length())); }
    private String safeFileName(String value) { return value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_"); }
    private void ensureInside(Path root, Path path) { if (!path.toAbsolutePath().normalize().startsWith(root.toAbsolutePath().normalize())) throw new IllegalStateException("归档路径越界"); }
    private Long longValue(Object value) { if (value instanceof Number) return ((Number) value).longValue(); try { return value == null ? null : Long.valueOf(String.valueOf(value)); } catch (Exception e) { return null; } }
    private Integer intValue(Object value) { if (value instanceof Number) return ((Number) value).intValue(); try { return value == null ? null : Integer.valueOf(String.valueOf(value)); } catch (Exception e) { return null; } }
    private int defaultInt(Object value, int fallback) { Integer parsed = intValue(value); return parsed == null ? fallback : parsed; }
    private Double doubleValue(Object value) { if (value instanceof Number) return ((Number) value).doubleValue(); try { return value == null ? null : Double.valueOf(String.valueOf(value)); } catch (Exception e) { return null; } }
    private String stringValue(Object value, String fallback) { return value == null ? fallback : String.valueOf(value); }
    private String sha256(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192]; int read;
            while ((read = in.read(buffer)) > 0) digest.update(buffer, 0, read);
            StringBuilder out = new StringBuilder(); for (byte b : digest.digest()) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception e) { throw new IllegalStateException("归档文件哈希计算失败", e); }
    }

    private static Map<Integer, String> createCatalog() {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(2, "发票回执等收费凭证"); map.put(3, "委托代理合同"); map.put(4, "授权委托书");
        map.put(5, "起诉状、上诉状或答辩状"); map.put(6, "阅卷笔录、会见当事人谈话笔录");
        map.put(7, "证据材料"); map.put(8, "诉讼保全、证据保全及相关裁判文书"); map.put(9, "承办律师代理意见");
        map.put(10, "集体讨论记录"); map.put(11, "代理词或辩护词"); map.put(12, "出庭通知书或传票");
        map.put(13, "庭审笔录"); map.put(14, "裁定书、判决书、调解书"); map.put(15, "执行申请书及执行文书");
        return Collections.unmodifiableMap(map);
    }
}
