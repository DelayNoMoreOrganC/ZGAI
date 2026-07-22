package com.lawfirm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.LegacyMaterialSearchRequest;
import com.lawfirm.dto.LegacyMaterialSearchResponse;
import com.lawfirm.dto.LegacyMaterialSearchResultDTO;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.Client;
import com.lawfirm.entity.Department;
import com.lawfirm.entity.LegacyMaterialSearchRecord;
import com.lawfirm.entity.LegacyMaterialSearchResult;
import com.lawfirm.entity.Party;
import com.lawfirm.entity.User;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.exception.ResourceNotFoundException;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.ClientRepository;
import com.lawfirm.repository.DepartmentRepository;
import com.lawfirm.repository.LegacyMaterialSearchRecordRepository;
import com.lawfirm.repository.LegacyMaterialSearchResultRepository;
import com.lawfirm.repository.PartyRepository;
import com.lawfirm.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 基于当前用户可见案件的强识别要素检索旧案材料。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LegacyMaterialSearchService {

    private final CaseRepository caseRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PartyRepository partyRepository;
    private final LegacyMaterialSearchRecordRepository recordRepository;
    private final LegacyMaterialSearchResultRepository resultRepository;
    private final CaseService caseService;
    private final ObjectMapper objectMapper;

    @Value("${legacy.case-archive.root-path:}")
    private String legacyArchiveRootPath;

    @Value("${legacy.case-archive.max-scan-results:30}")
    private int maxScanResults;

    @Value("${legacy.case-archive.max-scan-files:100000}")
    private int maxScanFiles;

    @Transactional
    public LegacyMaterialSearchResponse search(LegacyMaterialSearchRequest request, Long currentUserId) {
        if (request == null || request.getCaseId() == null) {
            throw new InvalidParameterException("caseId", "请选择有权查看的来源案件");
        }
        caseService.assertCaseVisible(request.getCaseId(), currentUserId);

        Case sourceCase = caseRepository.findById(request.getCaseId())
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("案件", request.getCaseId()));
        SearchContext context = buildContext(sourceCase);
        if (context.archiveTerms.isEmpty()) {
            throw new InvalidParameterException("caseId", "案件缺少案号、案件名称、客户或当事人等可检索要素");
        }

        LegacyMaterialSearchRecord record = new LegacyMaterialSearchRecord();
        record.setKeyword(firstTermValue(context.archiveTerms));
        record.setQueryParams(buildQueryParams(sourceCase));
        record.setSearchedBy(currentUserId);
        record.setSourceCaseId(sourceCase.getId());
        record.setResultCount(0);
        record.setArchivePathConfigured(isArchivePathConfigured());
        record = recordRepository.save(record);

        List<LegacyMaterialSearchResultDTO> results =
                searchArchiveFiles(context, record.getId(), normalizeLimit(request.getLimit()));
        record.setResultCount(results.size());
        recordRepository.save(record);

        LegacyMaterialSearchResponse response = new LegacyMaterialSearchResponse();
        response.setRecordId(record.getId());
        response.setArchivePathConfigured(isArchivePathConfigured());
        response.setSourceCaseId(sourceCase.getId());
        response.setSourceCaseName(sourceCase.getCaseName());
        response.setSourceCaseNumber(sourceCase.getCaseNumber());
        response.setSearchElements(context.displayElements);
        response.setResults(results);
        response.setTotal(results.size());
        response.setMessage(buildMessage(results.size()));
        return response;
    }

    @Transactional(readOnly = true)
    public FileDownload loadDownload(Long resultId, Long currentUserId) {
        LegacyMaterialSearchResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("旧资料文件", resultId));
        caseService.assertCaseVisible(result.getSourceCaseId(), currentUserId);

        Path root = resolveArchiveRoot();
        Path candidate = root.resolve(result.getRelativePath()).normalize();
        if (!candidate.startsWith(root)) {
            throw new InvalidParameterException("resultId", "资料路径不合法");
        }
        try {
            Path realPath = candidate.toRealPath();
            if (!realPath.startsWith(root) || !Files.isRegularFile(realPath)) {
                throw new InvalidParameterException("resultId", "资料文件不可用");
            }
            return new FileDownload(realPath, result.getFileName());
        } catch (IOException e) {
            throw new InvalidParameterException("resultId", "资料文件不存在或暂时不可访问");
        }
    }

    private SearchContext buildContext(Case sourceCase) {
        SearchContext context = newContext(sourceCase);
        addArchiveTerm(context, "ZGAI案号", sourceCase.getCaseNumber(), 1.0);
        addArchiveTerm(context, "法院案号", sourceCase.getCourtCaseNumber(), 1.0);
        addArchiveTerm(context, "案件名称", sourceCase.getCaseName(), 0.95);
        addDisplayElement(context, "案由", sourceCase.getCaseReason());
        addDisplayElement(context, "受理单位", sourceCase.getCourt());

        if (sourceCase.getClientId() != null) {
            Optional<Client> client = clientRepository.findById(sourceCase.getClientId())
                    .filter(item -> !Boolean.TRUE.equals(item.getDeleted()));
            client.ifPresent(item -> addArchiveTerm(context, "客户", item.getClientName(), 0.95));
        }

        for (Party party : partyRepository.findByCaseIdAndDeletedFalse(sourceCase.getId())) {
            addArchiveTerm(context, "当事人", party.getName(), 0.9);
        }

        User owner = sourceCase.getOwnerId() == null
                ? null
                : userRepository.findById(sourceCase.getOwnerId()).orElse(null);
        if (owner != null) {
            addDisplayElement(context, "承办人", owner.getRealName());
            Department department = owner.getDepartmentId() == null
                    ? null
                    : departmentRepository.findById(owner.getDepartmentId()).orElse(null);
            if (department != null) {
                addDisplayElement(context, "部门", department.getDeptName());
            }
        }
        return context;
    }

    private List<LegacyMaterialSearchResultDTO> searchArchiveFiles(
            SearchContext context, Long recordId, int limit) {
        if (!isArchivePathConfigured()) {
            return Collections.emptyList();
        }

        Path root;
        try {
            root = resolveArchiveRoot();
        } catch (InvalidParameterException e) {
            log.warn("旧资料目录不可用");
            return Collections.emptyList();
        }

        int resultLimit = Math.min(limit, Math.max(1, maxScanResults));
        List<LegacyMaterialSearchResultDTO> results = new ArrayList<>();
        int scanned = 0;
        try (Stream<Path> stream = Files.walk(root, 6)) {
            java.util.Iterator<Path> iterator = stream
                    .filter(path -> !Files.isSymbolicLink(path))
                    .filter(Files::isRegularFile)
                    .iterator();
            while (iterator.hasNext() && results.size() < resultLimit && scanned < Math.max(1, maxScanFiles)) {
                Path path = iterator.next();
                scanned++;
                Path relativePath = root.relativize(path);
                Match match = matchPath(relativePath, context.archiveTerms);
                if (match.score <= 0) {
                    continue;
                }

                LegacyMaterialSearchResult stored = new LegacyMaterialSearchResult();
                stored.setSearchRecordId(recordId);
                stored.setSourceCaseId(context.sourceCase.getId());
                stored.setRelativePath(relativePath.toString());
                stored.setFileName(path.getFileName().toString());
                stored.setFileSize(safeFileSize(path));
                stored.setLastModifiedAt(safeLastModified(path));
                stored = resultRepository.save(stored);

                LegacyMaterialSearchResultDTO dto = new LegacyMaterialSearchResultDTO();
                dto.setSourceType("LEGACY_FILE");
                dto.setRelatedId(context.sourceCase.getId());
                dto.setLegacyFileId(stored.getId());
                dto.setTitle(stored.getFileName());
                dto.setCaseNumber(context.sourceCase.getCaseNumber());
                dto.setCaseReason(context.sourceCase.getCaseReason());
                dto.setOwnerName(findOwnerName(context.sourceCase.getOwnerId()));
                dto.setDepartmentName(findDepartmentName(context.sourceCase.getOwnerId()));
                dto.setFileSize(stored.getFileSize());
                dto.setLastModifiedAt(stored.getLastModifiedAt());
                dto.setDownloadable(true);
                dto.setMatchReason(match.reason);
                dto.setScore(match.score);
                results.add(dto);
            }
        } catch (IOException e) {
            log.warn("扫描旧资料目录失败: {}", e.getClass().getSimpleName());
        }
        return results;
    }

    private Match matchPath(Path relativePath, List<SearchTerm> terms) {
        String searchable = relativePath.toString().toLowerCase(Locale.ROOT);
        Match match = new Match();
        for (SearchTerm term : terms) {
            if (searchable.contains(term.value.toLowerCase(Locale.ROOT))) {
                match.add(term.weight, term.label + "：" + term.value);
            }
        }
        return match;
    }

    private void addArchiveTerm(SearchContext context, String label, String value, double weight) {
        if (!isStrongIdentityTerm(value)) {
            return;
        }
        String normalized = value.trim();
        String dedupeKey = normalized.toLowerCase(Locale.ROOT);
        if (context.termKeys.add(dedupeKey)) {
            context.archiveTerms.add(new SearchTerm(label, normalized, weight));
        }
        addDisplayElement(context, label, normalized);
    }

    private void addDisplayElement(SearchContext context, String label, String value) {
        if (!hasText(value)) {
            return;
        }
        String element = label + "：" + value.trim();
        if (!context.displayElements.contains(element)) {
            context.displayElements.add(element);
        }
    }

    private boolean isStrongIdentityTerm(String value) {
        if (!hasText(value)) {
            return false;
        }
        String normalized = value.trim();
        return normalized.length() >= 2 || normalized.chars().anyMatch(Character::isDigit);
    }

    private String findOwnerName(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        return userRepository.findById(ownerId).map(User::getRealName).orElse(null);
    }

    private String findDepartmentName(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        User owner = userRepository.findById(ownerId).orElse(null);
        if (owner == null || owner.getDepartmentId() == null) {
            return null;
        }
        return departmentRepository.findById(owner.getDepartmentId())
                .map(Department::getDeptName)
                .orElse(null);
    }

    private Long safeFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return null;
        }
    }

    private LocalDateTime safeLastModified(Path path) {
        try {
            Instant instant = Files.getLastModifiedTime(path).toInstant();
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (IOException e) {
            return null;
        }
    }

    private Path resolveArchiveRoot() {
        if (!isArchivePathConfigured()) {
            throw new InvalidParameterException("archive", "旧资料目录尚未配置");
        }
        Path configured = Paths.get(legacyArchiveRootPath).toAbsolutePath().normalize();
        try {
            Path root = configured.toRealPath();
            if (!Files.isDirectory(root)) {
                throw new InvalidParameterException("archive", "旧资料目录不可用");
            }
            return root;
        } catch (IOException e) {
            throw new InvalidParameterException("archive", "旧资料目录不可用");
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 30;
        }
        return Math.min(limit, 100);
    }

    private boolean isArchivePathConfigured() {
        return hasText(legacyArchiveRootPath);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String firstTermValue(List<SearchTerm> terms) {
        return terms.isEmpty() ? "" : terms.get(0).value;
    }

    private String buildMessage(int count) {
        if (!isArchivePathConfigured()) {
            return "已提取来源案件检索要素；旧系统资料目录尚未配置。";
        }
        return count > 0 ? "已按来源案件要素找到旧案材料。" : "未找到与该案件要素匹配的旧案材料。";
    }

    private String buildQueryParams(Case sourceCase) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("caseId", sourceCase.getId());
        params.put("caseNumber", sourceCase.getCaseNumber());
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            return "{\"caseId\":" + sourceCase.getId() + "}";
        }
    }

    private static class SearchContext {
        private Case sourceCase;
        private final List<SearchTerm> archiveTerms = new ArrayList<>();
        private final Set<String> termKeys = new LinkedHashSet<>();
        private final List<String> displayElements = new ArrayList<>();
    }

    private SearchContext newContext(Case sourceCase) {
        SearchContext context = new SearchContext();
        context.sourceCase = sourceCase;
        return context;
    }

    private static class SearchTerm {
        private final String label;
        private final String value;
        private final double weight;

        private SearchTerm(String label, String value, double weight) {
            this.label = label;
            this.value = value;
            this.weight = weight;
        }
    }

    private static class Match {
        private double score;
        private String reason = "";

        private void add(double value, String label) {
            score += value;
            reason = reason.isEmpty() ? label : reason + "、" + label;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class FileDownload {
        private final Path path;
        private final String fileName;
    }
}
