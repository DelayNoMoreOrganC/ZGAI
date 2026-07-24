package com.lawfirm.service;

import com.lawfirm.dto.RagEvaluationCaseDTO;
import com.lawfirm.dto.RagEvaluationCaseRequest;
import com.lawfirm.dto.RagEvaluationRunDTO;
import com.lawfirm.entity.KnowledgeArticle;
import com.lawfirm.entity.RagEvaluationCase;
import com.lawfirm.entity.RagEvaluationRun;
import com.lawfirm.exception.BusinessException;
import com.lawfirm.repository.KnowledgeArticleRepository;
import com.lawfirm.repository.RagEvaluationCaseRepository;
import com.lawfirm.repository.RagEvaluationRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagEvaluationService {
    private final RagEvaluationCaseRepository caseRepository;
    private final RagEvaluationRunRepository runRepository;
    private final KnowledgeArticleRepository articleRepository;
    private final RAGKnowledgeService ragKnowledgeService;

    @Transactional(readOnly = true)
    public List<RagEvaluationCaseDTO> listCases() {
        return caseRepository.findTop100ByDeletedFalseOrderByCreatedAtDesc().stream()
                .map(this::toCaseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCandidateArticles() {
        return articleRepository.findAll().stream()
                .filter(article -> !Boolean.TRUE.equals(article.getDeleted()))
                .sorted(Comparator.comparing(KnowledgeArticle::getId).reversed())
                .limit(500)
                .map(article -> {
                    Map<String, Object> candidate = new LinkedHashMap<>();
                    candidate.put("id", article.getId());
                    candidate.put("title", article.getTitle());
                    candidate.put("knowledgeSource", article.getKnowledgeSource());
                    candidate.put("ragIndexable", KnowledgeArticlePolicy.isRagIndexable(article));
                    candidate.put("public", Boolean.TRUE.equals(article.getIsPublic()));
                    return candidate;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public RagEvaluationCaseDTO createCase(RagEvaluationCaseRequest request, Long userId) {
        List<Long> expected = normalizeIds(request.getExpectedArticleIds());
        List<Long> forbidden = normalizeIds(request.getForbiddenArticleIds());
        validateArticleSets(expected, forbidden);

        RagEvaluationCase evaluationCase = new RagEvaluationCase();
        applyRequest(evaluationCase, request, expected, forbidden);
        evaluationCase.setCreatedBy(userId);
        return toCaseDto(caseRepository.save(evaluationCase));
    }

    @Transactional
    public RagEvaluationCaseDTO updateCase(Long id, RagEvaluationCaseRequest request) {
        RagEvaluationCase evaluationCase = getActiveCase(id);
        List<Long> expected = normalizeIds(request.getExpectedArticleIds());
        List<Long> forbidden = normalizeIds(request.getForbiddenArticleIds());
        validateArticleSets(expected, forbidden);
        applyRequest(evaluationCase, request, expected, forbidden);
        return toCaseDto(caseRepository.save(evaluationCase));
    }

    @Transactional
    public void deleteCase(Long id) {
        RagEvaluationCase evaluationCase = getActiveCase(id);
        evaluationCase.setDeleted(true);
        caseRepository.save(evaluationCase);
    }

    @Transactional
    public Map<String, Object> runEnabledCases(Long userId) {
        List<RagEvaluationCase> cases = caseRepository.findByDeletedFalseAndEnabledTrueOrderByCreatedAtAsc();
        if (cases.isEmpty()) {
            throw BusinessException.validationError("请先建立并启用至少一个RAG评价样本");
        }

        List<RagEvaluationRunDTO> runs = new ArrayList<>();
        for (RagEvaluationCase evaluationCase : cases) {
            runs.add(runCase(evaluationCase, userId));
        }
        long passed = runs.stream().filter(run -> Boolean.TRUE.equals(run.getPassed())).count();
        long top3Hits = runs.stream().filter(run -> Boolean.TRUE.equals(run.getTop3Hit())).count();
        long forbiddenHits = runs.stream().filter(run -> Boolean.TRUE.equals(run.getForbiddenHit())).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", runs.size());
        result.put("passed", passed);
        result.put("failed", runs.size() - passed);
        result.put("top3HitRate", roundRate(top3Hits, runs.size()));
        result.put("forbiddenHitCount", forbiddenHits);
        result.put("privacyBoundaryPassed", forbiddenHits == 0);
        result.put("runs", runs);
        return result;
    }

    @Transactional(readOnly = true)
    public List<RagEvaluationRunDTO> listRuns() {
        List<RagEvaluationRun> runs = runRepository.findTop100ByOrderByCreatedAtDesc();
        Map<Long, RagEvaluationCase> cases = caseRepository.findAllById(
                        runs.stream()
                                .map(RagEvaluationRun::getEvaluationCaseId)
                                .collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(RagEvaluationCase::getId, Function.identity()));
        return runs.stream()
                .map(run -> toRunDto(run, Optional.ofNullable(cases.get(run.getEvaluationCaseId()))
                        .map(RagEvaluationCase::getName).orElse("已删除样本")))
                .collect(Collectors.toList());
    }

    private RagEvaluationRunDTO runCase(RagEvaluationCase evaluationCase, Long userId) {
        List<Long> expected = parseIds(evaluationCase.getExpectedArticleIds());
        Set<Long> forbidden = new HashSet<>(parseIds(evaluationCase.getForbiddenArticleIds()));
        RAGKnowledgeService.RetrievalSnapshot snapshot =
                ragKnowledgeService.evaluateRetrieval(evaluationCase.getQuestion());
        List<Long> retrieved = snapshot.getArticleIds();

        boolean top3Hit = retrieved.stream().limit(3).anyMatch(expected::contains);
        boolean forbiddenHit = retrieved.stream().anyMatch(forbidden::contains);

        RagEvaluationRun run = new RagEvaluationRun();
        run.setEvaluationCaseId(evaluationCase.getId());
        run.setRetrievedArticleIds(joinIds(retrieved));
        run.setSearchMethod(snapshot.getSearchMethod());
        run.setTop3Hit(top3Hit);
        run.setForbiddenHit(forbiddenHit);
        run.setPassed(top3Hit && !forbiddenHit);
        run.setDurationMs(snapshot.getDurationMs());
        run.setRunBy(userId);
        return toRunDto(runRepository.save(run), evaluationCase.getName());
    }

    private void validateArticleSets(List<Long> expected, List<Long> forbidden) {
        if (expected.isEmpty()) {
            throw BusinessException.validationError("至少选择一篇预期命中文档");
        }
        Set<Long> overlap = new HashSet<>(expected);
        overlap.retainAll(forbidden);
        if (!overlap.isEmpty()) {
            throw BusinessException.validationError("同一文档不能同时标记为预期命中和禁止命中");
        }
        Set<Long> requestedIds = new HashSet<>(expected);
        requestedIds.addAll(forbidden);
        Map<Long, KnowledgeArticle> articles = articleRepository.findAllById(requestedIds).stream()
                .collect(Collectors.toMap(KnowledgeArticle::getId, Function.identity()));
        if (articles.size() != requestedIds.size()) {
            throw BusinessException.validationError("评价样本引用了不存在的知识文档");
        }
        for (Long id : expected) {
            if (!KnowledgeArticlePolicy.isRagIndexable(articles.get(id))) {
                throw BusinessException.validationError("预期命中文档必须已审核并允许进入共享RAG：" + id);
            }
        }
    }

    private void applyRequest(RagEvaluationCase target, RagEvaluationCaseRequest request,
                              List<Long> expected, List<Long> forbidden) {
        target.setName(request.getName().trim());
        target.setQuestion(request.getQuestion().trim());
        target.setExpectedArticleIds(joinIds(expected));
        target.setForbiddenArticleIds(joinIds(forbidden));
        target.setEnabled(!Boolean.FALSE.equals(request.getEnabled()));
    }

    private RagEvaluationCase getActiveCase(Long id) {
        RagEvaluationCase evaluationCase = caseRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("RAG评价样本不存在"));
        if (Boolean.TRUE.equals(evaluationCase.getDeleted())) {
            throw BusinessException.notFound("RAG评价样本不存在");
        }
        return evaluationCase;
    }

    private List<Long> normalizeIds(List<Long> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .filter(value -> value > 0)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private String joinIds(Collection<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<Long> parseIds(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    private double roundRate(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        return Math.round((numerator * 10000.0) / denominator) / 100.0;
    }

    private RagEvaluationCaseDTO toCaseDto(RagEvaluationCase source) {
        return RagEvaluationCaseDTO.builder()
                .id(source.getId())
                .name(source.getName())
                .question(source.getQuestion())
                .expectedArticleIds(parseIds(source.getExpectedArticleIds()))
                .forbiddenArticleIds(parseIds(source.getForbiddenArticleIds()))
                .enabled(source.getEnabled())
                .createdBy(source.getCreatedBy())
                .createdAt(source.getCreatedAt())
                .build();
    }

    private RagEvaluationRunDTO toRunDto(RagEvaluationRun source, String caseName) {
        return RagEvaluationRunDTO.builder()
                .id(source.getId())
                .evaluationCaseId(source.getEvaluationCaseId())
                .caseName(caseName)
                .retrievedArticleIds(parseIds(source.getRetrievedArticleIds()))
                .searchMethod(source.getSearchMethod())
                .top3Hit(source.getTop3Hit())
                .forbiddenHit(source.getForbiddenHit())
                .passed(source.getPassed())
                .durationMs(source.getDurationMs())
                .runBy(source.getRunBy())
                .createdAt(source.getCreatedAt())
                .build();
    }
}
