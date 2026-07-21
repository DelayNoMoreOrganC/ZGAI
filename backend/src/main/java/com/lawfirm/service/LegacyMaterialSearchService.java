package com.lawfirm.service;

import com.lawfirm.dto.LegacyMaterialSearchRequest;
import com.lawfirm.dto.LegacyMaterialSearchResponse;
import com.lawfirm.dto.LegacyMaterialSearchResultDTO;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.Client;
import com.lawfirm.entity.Department;
import com.lawfirm.entity.LegacyMaterialSearchRecord;
import com.lawfirm.entity.User;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.ClientRepository;
import com.lawfirm.repository.DepartmentRepository;
import com.lawfirm.repository.LegacyMaterialSearchRecordRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 旧系统资料检索。
 *
 * 第一阶段只建立入口和留痕：基于 ZGAI 现有案件/客户字段生成检索线索；
 * 如配置旧资料根目录，则同步扫描文件名/路径命中项。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LegacyMaterialSearchService {

    private final CaseRepository caseRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final LegacyMaterialSearchRecordRepository recordRepository;

    @Value("${legacy.case-archive.root-path:}")
    private String legacyArchiveRootPath;

    @Value("${legacy.case-archive.max-scan-results:30}")
    private int maxScanResults;

    @Transactional
    public LegacyMaterialSearchResponse search(LegacyMaterialSearchRequest request, Long currentUserId) {
        int limit = normalizeLimit(request.getLimit());
        Map<Long, User> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
        Map<Long, Department> departmentMap = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, department -> department, (a, b) -> a));

        List<LegacyMaterialSearchResultDTO> results = new ArrayList<>();
        results.addAll(searchCurrentCases(request, userMap, departmentMap, limit));
        results.addAll(searchCurrentClients(request, userMap, departmentMap, limit));
        results.addAll(searchArchiveFiles(request, limit));

        results = results.stream()
                .sorted((a, b) -> Double.compare(valueOrZero(b.getScore()), valueOrZero(a.getScore())))
                .limit(limit)
                .collect(Collectors.toList());

        LegacyMaterialSearchRecord record = new LegacyMaterialSearchRecord();
        record.setKeyword(firstNonBlank(request.getKeyword(), request.getCaseName(), request.getCaseNumber(), request.getClientName()));
        record.setQueryParams(buildQueryParams(request));
        record.setSearchedBy(currentUserId);
        record.setResultCount(results.size());
        record.setArchivePathConfigured(isArchivePathConfigured());
        record = recordRepository.save(record);

        LegacyMaterialSearchResponse response = new LegacyMaterialSearchResponse();
        response.setRecordId(record.getId());
        response.setArchivePathConfigured(isArchivePathConfigured());
        response.setArchiveRootPath(isArchivePathConfigured() ? legacyArchiveRootPath : null);
        response.setResults(results);
        response.setTotal(results.size());
        response.setMessage(buildMessage(results.size()));
        return response;
    }

    private List<LegacyMaterialSearchResultDTO> searchCurrentCases(LegacyMaterialSearchRequest request,
                                                                   Map<Long, User> userMap,
                                                                   Map<Long, Department> departmentMap,
                                                                   int limit) {
        List<LegacyMaterialSearchResultDTO> results = new ArrayList<>();
        for (Case item : caseRepository.findByDeletedFalse()) {
            Match match = matchCase(item, request, userMap, departmentMap);
            if (match.score <= 0) {
                continue;
            }
            LegacyMaterialSearchResultDTO dto = new LegacyMaterialSearchResultDTO();
            dto.setSourceType("ZGAI_CASE");
            dto.setRelatedId(item.getId());
            dto.setTitle(item.getCaseName());
            dto.setCaseNumber(item.getCaseNumber());
            dto.setCaseReason(item.getCaseReason());
            dto.setMaterialPath(item.getCaseFolderPath());
            User owner = userMap.get(item.getOwnerId());
            if (owner != null) {
                dto.setOwnerName(owner.getRealName());
                Department dept = departmentMap.get(owner.getDepartmentId());
                if (dept != null) {
                    dto.setDepartmentName(dept.getDeptName());
                }
            }
            dto.setMatchReason(match.reason);
            dto.setScore(match.score);
            results.add(dto);
            if (results.size() >= limit) {
                break;
            }
        }
        return results;
    }

    private List<LegacyMaterialSearchResultDTO> searchCurrentClients(LegacyMaterialSearchRequest request,
                                                                     Map<Long, User> userMap,
                                                                     Map<Long, Department> departmentMap,
                                                                     int limit) {
        List<LegacyMaterialSearchResultDTO> results = new ArrayList<>();
        for (Client item : clientRepository.findByDeletedFalse()) {
            Match match = matchClient(item, request, userMap, departmentMap);
            if (match.score <= 0) {
                continue;
            }
            LegacyMaterialSearchResultDTO dto = new LegacyMaterialSearchResultDTO();
            dto.setSourceType("ZGAI_CLIENT");
            dto.setRelatedId(item.getId());
            dto.setTitle(item.getClientName());
            dto.setClientName(item.getClientName());
            User owner = userMap.get(item.getOwnerId());
            if (owner != null) {
                dto.setOwnerName(owner.getRealName());
            }
            Department dept = departmentMap.get(item.getDepartmentId());
            if (dept != null) {
                dto.setDepartmentName(dept.getDeptName());
            }
            dto.setMatchReason(match.reason);
            dto.setScore(match.score);
            results.add(dto);
            if (results.size() >= limit) {
                break;
            }
        }
        return results;
    }

    private List<LegacyMaterialSearchResultDTO> searchArchiveFiles(LegacyMaterialSearchRequest request, int limit) {
        if (!isArchivePathConfigured()) {
            return Collections.emptyList();
        }

        Path root = Paths.get(legacyArchiveRootPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            log.warn("旧资料目录不可用: {}", root);
            return Collections.emptyList();
        }

        List<String> terms = searchTerms(request);
        if (terms.isEmpty()) {
            return Collections.emptyList();
        }

        List<LegacyMaterialSearchResultDTO> results = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root, 6)) {
            Iterator<Path> iterator = stream
                    .filter(Files::isRegularFile)
                    .iterator();
            while (iterator.hasNext() && results.size() < Math.min(limit, maxScanResults)) {
                Path path = iterator.next();
                String relativePath = root.relativize(path).toString();
                Match match = matchText(relativePath, terms, "旧资料路径");
                if (match.score <= 0) {
                    continue;
                }
                LegacyMaterialSearchResultDTO dto = new LegacyMaterialSearchResultDTO();
                dto.setSourceType("LEGACY_FILE");
                dto.setTitle(path.getFileName().toString());
                dto.setMaterialPath(path.toString());
                dto.setMatchReason(match.reason);
                dto.setScore(match.score);
                results.add(dto);
            }
        } catch (IOException e) {
            log.warn("扫描旧资料目录失败: {}", e.getMessage());
        }
        return results;
    }

    private Match matchCase(Case item, LegacyMaterialSearchRequest request,
                            Map<Long, User> userMap,
                            Map<Long, Department> departmentMap) {
        Match match = new Match();
        match.add(matchField(item.getCaseName(), request.getCaseName(), "案件名称", 1.0));
        match.add(matchField(item.getCaseNumber(), request.getCaseNumber(), "案号", 1.0));
        match.add(matchField(item.getCaseReason(), request.getCaseReason(), "案由", 0.9));
        match.add(matchField(item.getCaseName(), request.getKeyword(), "关键词命中案件名称", 0.7));
        match.add(matchField(item.getCaseNumber(), request.getKeyword(), "关键词命中案号", 0.7));
        match.add(matchField(item.getCaseReason(), request.getKeyword(), "关键词命中案由", 0.6));
        match.add(matchField(item.getCourt(), request.getKeyword(), "关键词命中受理单位", 0.4));

        User owner = userMap.get(item.getOwnerId());
        if (owner != null) {
            match.add(matchField(owner.getRealName(), request.getOwnerName(), "承办人", 0.7));
            match.add(matchField(owner.getRealName(), request.getKeyword(), "关键词命中承办人", 0.4));
            Department dept = departmentMap.get(owner.getDepartmentId());
            if (dept != null) {
                match.add(matchField(dept.getDeptName(), request.getDepartmentName(), "部门", 0.6));
                match.add(matchField(dept.getDeptName(), request.getKeyword(), "关键词命中部门", 0.3));
            }
        }
        return match;
    }

    private Match matchClient(Client item, LegacyMaterialSearchRequest request,
                              Map<Long, User> userMap,
                              Map<Long, Department> departmentMap) {
        Match match = new Match();
        match.add(matchField(item.getClientName(), request.getClientName(), "客户名称", 1.0));
        match.add(matchField(item.getClientName(), request.getKeyword(), "关键词命中客户名称", 0.7));
        match.add(matchField(item.getCreditCode(), request.getKeyword(), "关键词命中统一社会信用代码", 0.8));
        match.add(matchField(item.getIdCard(), request.getKeyword(), "关键词命中证件号", 0.8));

        User owner = userMap.get(item.getOwnerId());
        if (owner != null) {
            match.add(matchField(owner.getRealName(), request.getOwnerName(), "承办人", 0.7));
        }
        Department dept = departmentMap.get(item.getDepartmentId());
        if (dept != null) {
            match.add(matchField(dept.getDeptName(), request.getDepartmentName(), "部门", 0.6));
            match.add(matchField(dept.getDeptName(), request.getKeyword(), "关键词命中部门", 0.3));
        }
        return match;
    }

    private Match matchText(String text, List<String> terms, String label) {
        Match match = new Match();
        for (String term : terms) {
            if (contains(text, term)) {
                match.add(new Match(0.6, label + "：" + term));
            }
        }
        return match;
    }

    private Match matchField(String value, String query, String label, double score) {
        if (!contains(value, query)) {
            return new Match();
        }
        return new Match(score, label);
    }

    private boolean contains(String value, String query) {
        if (!hasText(value) || !hasText(query)) {
            return false;
        }
        return value.trim().toLowerCase().contains(query.trim().toLowerCase());
    }

    private List<String> searchTerms(LegacyMaterialSearchRequest request) {
        List<String> terms = new ArrayList<>();
        addTerm(terms, request.getKeyword());
        addTerm(terms, request.getCaseName());
        addTerm(terms, request.getCaseNumber());
        addTerm(terms, request.getClientName());
        addTerm(terms, request.getCaseReason());
        addTerm(terms, request.getOwnerName());
        addTerm(terms, request.getDepartmentName());
        return terms;
    }

    private void addTerm(List<String> terms, String term) {
        if (hasText(term) && !terms.contains(term.trim())) {
            terms.add(term.trim());
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 30;
        }
        return Math.min(limit, 100);
    }

    private double valueOrZero(Double value) {
        return value == null ? 0 : value;
    }

    private boolean isArchivePathConfigured() {
        return hasText(legacyArchiveRootPath);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String buildMessage(int count) {
        if (!isArchivePathConfigured()) {
            return "已基于 ZGAI 当前案件/客户库生成检索线索；旧系统资料目录尚未配置。";
        }
        return count > 0 ? "已检索 ZGAI 当前数据和旧资料目录。" : "未找到匹配资料。";
    }

    private String buildQueryParams(LegacyMaterialSearchRequest request) {
        return "{"
                + "\"keyword\":\"" + escape(request.getKeyword()) + "\","
                + "\"caseName\":\"" + escape(request.getCaseName()) + "\","
                + "\"caseNumber\":\"" + escape(request.getCaseNumber()) + "\","
                + "\"clientName\":\"" + escape(request.getClientName()) + "\","
                + "\"caseReason\":\"" + escape(request.getCaseReason()) + "\","
                + "\"ownerName\":\"" + escape(request.getOwnerName()) + "\","
                + "\"departmentName\":\"" + escape(request.getDepartmentName()) + "\""
                + "}";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class Match {
        private double score;
        private String reason;

        private Match() {
            this(0, "");
        }

        private Match(double score, String reason) {
            this.score = score;
            this.reason = reason;
        }

        private void add(Match other) {
            if (other == null || other.score <= 0) {
                return;
            }
            this.score += other.score;
            if (this.reason == null || this.reason.isEmpty()) {
                this.reason = other.reason;
            } else {
                this.reason = this.reason + "、" + other.reason;
            }
        }
    }
}
