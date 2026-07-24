package com.lawfirm.service;

import com.lawfirm.dto.RagEvaluationCaseRequest;
import com.lawfirm.dto.RagEvaluationImportResult;
import com.lawfirm.entity.KnowledgeArticle;
import com.lawfirm.repository.KnowledgeArticleRepository;
import com.lawfirm.repository.RagEvaluationCaseRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagEvaluationWorkbookService {
    private static final int MAX_FILE_BYTES = 2 * 1024 * 1024;
    private static final int MAX_ROWS = 200;
    private static final List<String> HEADERS = List.of(
            "样本名称", "评价问题", "预期文档ID", "禁止文档ID", "是否启用");

    private final RagEvaluationCaseRepository caseRepository;
    private final KnowledgeArticleRepository articleRepository;
    private final RagEvaluationService evaluationService;

    @Transactional(readOnly = true)
    public byte[] createTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            Sheet sampleSheet = workbook.createSheet("评价样本");
            Row header = sampleSheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS.get(i));
                cell.setCellStyle(headerStyle);
                sampleSheet.setColumnWidth(i, i == 1 ? 14000 : 6000);
            }
            Row example = sampleSheet.createRow(1);
            example.createCell(0).setCellValue("示例-劳动仲裁时效");
            example.createCell(1).setCellValue("申请劳动仲裁的时效是多久？");
            example.createCell(2).setCellValue("请填写文档清单中的ID");
            example.createCell(3).setCellValue("多个ID用逗号分隔，可留空");
            example.createCell(4).setCellValue("是");
            sampleSheet.createFreezePane(0, 1);

            Sheet documentSheet = workbook.createSheet("文档清单");
            String[] documentHeaders = {"文档ID", "标题", "来源", "允许作为预期文档"};
            Row documentHeader = documentSheet.createRow(0);
            for (int i = 0; i < documentHeaders.length; i++) {
                Cell cell = documentHeader.createCell(i);
                cell.setCellValue(documentHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            List<KnowledgeArticle> articles = activeArticles();
            for (int i = 0; i < articles.size(); i++) {
                KnowledgeArticle article = articles.get(i);
                Row row = documentSheet.createRow(i + 1);
                row.createCell(0).setCellValue(article.getId());
                row.createCell(1).setCellValue(safe(article.getTitle()));
                row.createCell(2).setCellValue(safe(article.getKnowledgeSource()));
                row.createCell(3).setCellValue(KnowledgeArticlePolicy.isRagIndexable(article)
                        ? "是" : "否（仅可作为禁止文档）");
            }
            documentSheet.setColumnWidth(0, 3600);
            documentSheet.setColumnWidth(1, 16000);
            documentSheet.setColumnWidth(2, 6000);
            documentSheet.setColumnWidth(3, 8000);
            documentSheet.createFreezePane(0, 1);

            workbook.write(output);
            return output.toByteArray();
        } catch (Exception error) {
            throw new IllegalStateException("生成RAG评价模板失败", error);
        }
    }

    @Transactional
    public RagEvaluationImportResult importWorkbook(MultipartFile file, boolean dryRun, Long userId) {
        validateFile(file);
        ParsedWorkbook parsed = parse(file);
        Validation validation = validateRows(parsed.rows);
        int imported = 0;
        if (!dryRun && validation.canImport) {
            for (ParsedRow row : validation.validRows) {
                RagEvaluationCaseRequest request = new RagEvaluationCaseRequest();
                request.setName(row.name);
                request.setQuestion(row.question);
                request.setExpectedArticleIds(row.expectedIds);
                request.setForbiddenArticleIds(row.forbiddenIds);
                request.setEnabled(row.enabled);
                evaluationService.createCase(request, userId);
                imported++;
            }
        }
        return RagEvaluationImportResult.builder()
                .rowCount(parsed.rows.size())
                .validCount(validation.validRows.size())
                .skippedCount(validation.skippedCount)
                .importedCount(imported)
                .canImport(validation.canImport)
                .rows(validation.results)
                .build();
    }

    private ParsedWorkbook parse(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("评价样本");
            if (sheet == null) throw new IllegalArgumentException("工作簿缺少“评价样本”工作表");
            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            validateHeaders(sheet.getRow(0), formatter);
            List<ParsedRow> rows = new ArrayList<>();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || isBlankRow(row, formatter)) continue;
                if (rows.size() >= MAX_ROWS) {
                    throw new IllegalArgumentException("单次最多导入" + MAX_ROWS + "条评价样本");
                }
                rows.add(new ParsedRow(index + 1,
                        cell(row, 0, formatter), cell(row, 1, formatter),
                        cell(row, 2, formatter), cell(row, 3, formatter),
                        cell(row, 4, formatter)));
            }
            if (rows.isEmpty()) throw new IllegalArgumentException("评价样本工作表中没有可导入数据");
            return new ParsedWorkbook(rows);
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalArgumentException("无法读取评价样本，请使用系统下载的.xlsx模板", error);
        }
    }

    private Validation validateRows(List<ParsedRow> rows) {
        Map<Long, KnowledgeArticle> articles = activeArticles().stream()
                .collect(Collectors.toMap(KnowledgeArticle::getId, Function.identity()));
        Set<String> existingQuestions = caseRepository.findByDeletedFalse().stream()
                .map(item -> normalizeQuestion(item.getQuestion())).collect(Collectors.toSet());
        Set<String> workbookQuestions = new HashSet<>();
        List<ParsedRow> valid = new ArrayList<>();
        List<RagEvaluationImportResult.RowResult> results = new ArrayList<>();
        int skipped = 0;
        boolean hasErrors = false;

        for (ParsedRow row : rows) {
            String error = validateRow(row, articles);
            String normalizedQuestion = normalizeQuestion(row.question);
            if (error == null && !workbookQuestions.add(normalizedQuestion)) {
                error = "工作簿中存在重复评价问题";
            }
            if (error != null) {
                hasErrors = true;
                results.add(result(row, "ERROR", error));
            } else if (existingQuestions.contains(normalizedQuestion)) {
                skipped++;
                results.add(result(row, "SKIPPED", "系统中已存在相同评价问题"));
            } else {
                valid.add(row);
                results.add(result(row, "VALID", "校验通过"));
            }
        }
        return new Validation(valid, results, skipped, !hasErrors && !valid.isEmpty());
    }

    private String validateRow(ParsedRow row, Map<Long, KnowledgeArticle> articles) {
        row.name = trim(row.name);
        row.question = trim(row.question);
        if (row.name.isEmpty()) return "样本名称不能为空";
        if (row.name.length() > 120) return "样本名称不能超过120个字符";
        if (row.question.isEmpty()) return "评价问题不能为空";
        if (row.question.length() > 1000) return "评价问题不能超过1000个字符";
        try {
            row.expectedIds = parseIds(row.expectedRaw);
            row.forbiddenIds = parseIds(row.forbiddenRaw);
            row.enabled = parseEnabled(row.enabledRaw);
        } catch (IllegalArgumentException error) {
            return error.getMessage();
        }
        if (row.expectedIds.isEmpty()) return "至少填写一个预期文档ID";
        Set<Long> overlap = new HashSet<>(row.expectedIds);
        overlap.retainAll(row.forbiddenIds);
        if (!overlap.isEmpty()) return "同一文档不能同时作为预期和禁止文档";
        Set<Long> allIds = new HashSet<>(row.expectedIds);
        allIds.addAll(row.forbiddenIds);
        for (Long id : allIds) {
            if (!articles.containsKey(id)) return "文档ID不存在或已删除：" + id;
        }
        for (Long id : row.expectedIds) {
            if (!KnowledgeArticlePolicy.isRagIndexable(articles.get(id))) {
                return "预期文档尚未审核或禁止进入共享RAG：" + id;
            }
        }
        return null;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择评价样本文件");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx")) throw new IllegalArgumentException("评价样本仅支持.xlsx格式");
        if (file.getSize() > MAX_FILE_BYTES) throw new IllegalArgumentException("评价样本文件不能超过2MB");
    }

    private void validateHeaders(Row row, DataFormatter formatter) {
        if (row == null) throw new IllegalArgumentException("评价样本表头缺失");
        for (int i = 0; i < HEADERS.size(); i++) {
            if (!HEADERS.get(i).equals(cell(row, i, formatter))) {
                throw new IllegalArgumentException("第" + (i + 1) + "列表头必须为“" + HEADERS.get(i) + "”");
            }
        }
    }

    private List<KnowledgeArticle> activeArticles() {
        return articleRepository.findAll().stream()
                .filter(article -> !Boolean.TRUE.equals(article.getDeleted()))
                .sorted(Comparator.comparing(KnowledgeArticle::getId)).collect(Collectors.toList());
    }

    private List<Long> parseIds(String raw) {
        String value = trim(raw);
        if (value.isEmpty()) return new ArrayList<>();
        try {
            return Arrays.stream(value.replace('，', ',').split("[,;；\\s]+"))
                    .filter(item -> !item.isEmpty())
                    .map(item -> item.endsWith(".0") ? item.substring(0, item.length() - 2) : item)
                    .map(Long::valueOf).filter(id -> id > 0).distinct().sorted()
                    .collect(Collectors.toList());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("文档ID必须为数字，多个ID请用逗号分隔");
        }
    }

    private Boolean parseEnabled(String raw) {
        String value = trim(raw).toLowerCase(Locale.ROOT);
        if (value.isEmpty() || Set.of("是", "启用", "true", "1").contains(value)) return true;
        if (Set.of("否", "停用", "false", "0").contains(value)) return false;
        throw new IllegalArgumentException("是否启用只能填写是或否");
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (int i = 0; i < HEADERS.size(); i++) if (!cell(row, i, formatter).isEmpty()) return false;
        return true;
    }

    private String cell(Row row, int index, DataFormatter formatter) {
        Cell cell = row == null ? null : row.getCell(index);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private String normalizeQuestion(String value) {
        return trim(value).replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String trim(String value) { return value == null ? "" : value.trim(); }
    private String safe(String value) { return value == null ? "" : value; }

    private RagEvaluationImportResult.RowResult result(ParsedRow row, String status, String message) {
        return RagEvaluationImportResult.RowResult.builder().rowNumber(row.rowNumber)
                .name(row.name).question(row.question).status(status).message(message).build();
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static final class ParsedWorkbook {
        private final List<ParsedRow> rows;
        private ParsedWorkbook(List<ParsedRow> rows) { this.rows = rows; }
    }

    private static final class ParsedRow {
        private final int rowNumber;
        private String name;
        private String question;
        private final String expectedRaw;
        private final String forbiddenRaw;
        private final String enabledRaw;
        private List<Long> expectedIds = new ArrayList<>();
        private List<Long> forbiddenIds = new ArrayList<>();
        private Boolean enabled = true;
        private ParsedRow(int rowNumber, String name, String question, String expectedRaw,
                          String forbiddenRaw, String enabledRaw) {
            this.rowNumber = rowNumber; this.name = name; this.question = question;
            this.expectedRaw = expectedRaw; this.forbiddenRaw = forbiddenRaw; this.enabledRaw = enabledRaw;
        }
    }

    private static final class Validation {
        private final List<ParsedRow> validRows;
        private final List<RagEvaluationImportResult.RowResult> results;
        private final int skippedCount;
        private final boolean canImport;
        private Validation(List<ParsedRow> validRows, List<RagEvaluationImportResult.RowResult> results,
                           int skippedCount, boolean canImport) {
            this.validRows = validRows; this.results = results;
            this.skippedCount = skippedCount; this.canImport = canImport;
        }
    }
}
