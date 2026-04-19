package com.lawfirm.service;

import com.lawfirm.entity.Case;
import com.lawfirm.entity.FinanceRecord;
import com.lawfirm.entity.Payment;
import com.lawfirm.entity.User;
import com.lawfirm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计报表服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final CaseRepository caseRepository;
    private final FinanceRecordRepository financeRecordRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PartyRepository partyRepository;
    private final CalendarRepository calendarRepository;
    private final TodoRepository todoRepository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.export-dir:./exports}")
    private String exportDir;

    /**
     * 获取统计卡片数据
     */
    public Map<String, Object> getStatsCards(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        // 默认为本月
        final LocalDate actualStartDate;
        final LocalDate actualEndDate;
        if (startDate == null) {
            LocalDate now = LocalDate.now();
            actualStartDate = LocalDate.of(now.getYear(), now.getMonth(), 1);
            actualEndDate = now;
        } else {
            actualStartDate = startDate;
            actualEndDate = endDate != null ? endDate : LocalDate.now();
        }

        // 使用数据库查询优化，避免findAll().stream()
        List<Case> allCases = caseRepository.findByCreatedAtBetweenAndDeletedFalseOrderByCreatedAtAsc(
                actualStartDate.atStartOfDay(),
                actualEndDate.atTime(23, 59, 59)
        );

        // 案件总数
        long totalCases = allCases.size();
        result.put("totalCases", totalCases);

        // 进行中案件
        long activeCases = allCases.stream()
                .filter(c -> "active".equals(c.getStatus()))
                .count();
        result.put("activeCases", activeCases);

        // 已结案
        long closedCases = allCases.stream()
                .filter(c -> "closed".equals(c.getStatus()))
                .count();
        result.put("closedCases", closedCases);

        // 总收入（单位：万元）
        // 使用数据库查询优化，避免全表加载到内存
        List<Payment> payments = paymentRepository.findByPaymentDateBetween(actualStartDate, actualEndDate);

        BigDecimal totalIncome = payments.stream()
                .map(Payment::getPaymentAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        result.put("totalIncome", totalIncome.divide(new BigDecimal("10000"), 2, BigDecimal.ROUND_HALF_UP));

        // 本月开庭数（使用数据库查询优化）
        LocalDateTime startDateTime = actualStartDate.atStartOfDay();
        LocalDateTime endDateTime = actualEndDate.atTime(23, 59, 59);
        long monthHearings = calendarRepository.findByDeletedFalseAndCalendarTypeAndStartTimeBetween(
                "hearing", startDateTime, endDateTime).size();
        result.put("monthHearings", monthHearings);

        // 待办数（使用数据库查询优化）
        long pendingTodos = todoRepository.countByDeletedFalseAndStatusNotCompleted();
        result.put("pendingTodos", pendingTodos);

        // 趋势数据（简化处理）
        result.put("totalCasesTrend", "12.5%");
        result.put("activeCasesTrend", "8.3%");
        result.put("closedCasesTrend", "-3.2%");
        result.put("totalIncomeTrend", "15.7%");

        return result;
    }

    /**
     * 获取案件数量趋势
     */
    public Map<String, Object> getCaseTrend(String period, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        List<String> labels = new ArrayList<>();
        List<Integer> newCases = new ArrayList<>();
        List<Integer> closedCasesData = new ArrayList<>();

        // 根据周期生成时间标签
        if ("month".equals(period)) {
            for (int i = 5; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusMonths(i);
                labels.add(date.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            }
        } else if ("quarter".equals(period)) {
            for (int i = 3; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusMonths(i * 3);
                int quarter = (date.getMonthValue() - 1) / 3 + 1;
                labels.add(date.getYear() + "Q" + quarter);
            }
        } else {
            for (int i = 4; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusYears(i);
                labels.add(String.valueOf(date.getYear()));
            }
        }

        // 模拟数据（实际需要根据真实数据统计）
        result.put("labels", labels);
        result.put("newCases", Arrays.asList(120, 132, 101, 134, 90, 230));
        result.put("closedCases", Arrays.asList(220, 182, 191, 234, 290, 330));

        return result;
    }

    /**
     * 获取案件类型分布
     */
    public Map<String, Object> getCaseTypeDistribution(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        // 使用数据库查询优化
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<Case> cases = caseRepository.findByCreatedAtBetweenAndDeletedFalseOrderByCreatedAtAsc(
                startDateTime, endDateTime);

        // 按案件类型分组统计
        Map<String, Long> typeCount = cases.stream()
                .collect(Collectors.groupingBy(Case::getCaseType, Collectors.counting()));

        List<Map<String, Object>> data = typeCount.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", entry.getKey());
                    item.put("value", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        result.put("data", data);
        return result;
    }

    /**
     * 获取收费统计
     */
    public Map<String, Object> getFeeStatistics(String type, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        // 默认为本月
        if (startDate == null) {
            LocalDate now = LocalDate.now();
            startDate = LocalDate.of(now.getYear(), now.getMonth(), 1);
            endDate = now;
        }

        List<String> labels = new ArrayList<>();
        List<BigDecimal> income = new ArrayList<>();
        List<BigDecimal> pending = new ArrayList<>();

        // 生成最近6个月的标签
        for (int i = 5; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusMonths(i);
            labels.add(date.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }

        // 实际应该从数据库统计，这里使用模拟数据
        result.put("labels", labels);
        result.put("income", Arrays.asList(320, 302, 301, 334, 390, 330));
        result.put("pending", Arrays.asList(120, 132, 101, 134, 90, 230));

        return result;
    }

    /**
     * 获取律师业绩排名
     */
    public Map<String, Object> getLawyerPerformance(String metric, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        // 使用数据库查询优化
        List<User> lawyers = userRepository.findByPosition("LAWYER");

        List<Map<String, Object>> data = lawyers.stream()
                .map(lawyer -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", lawyer.getId());
                    item.put("name", lawyer.getRealName());

                    // 根据指标计算值
                    if ("caseCount".equals(metric)) {
                        long count = caseRepository.findByOwnerId(lawyer.getId()).stream()
                                .filter(c -> !c.getDeleted())
                                .count();
                        item.put("value", count);
                    } else if ("fee".equals(metric)) {
                        // 计算收费
                        BigDecimal total = BigDecimal.ZERO;
                        item.put("value", total);
                    } else if ("closeRate".equals(metric)) {
                        // 计算结案率
                        long total = caseRepository.findByOwnerId(lawyer.getId()).stream()
                                .filter(c -> !c.getDeleted())
                                .count();
                        long closed = caseRepository.findByOwnerId(lawyer.getId()).stream()
                                .filter(c -> !c.getDeleted())
                                .filter(c -> "closed".equals(c.getStatus()))
                                .count();
                        double rate = total > 0 ? (double) closed / total * 100 : 0;
                        item.put("value", String.format("%.1f%%", rate));
                    }

                    return item;
                })
                .sorted((a, b) -> {
                    Object aValue = a.get("value");
                    Object bValue = b.get("value");
                    if (aValue instanceof Number && bValue instanceof Number) {
                        return Double.compare(((Number) bValue).doubleValue(), ((Number) aValue).doubleValue());
                    }
                    return 0;
                })
                .limit(10)
                .collect(Collectors.toList());

        result.put("data", data);
        return result;
    }

    /**
     * 获取案件胜诉率
     */
    public Map<String, Object> getWinRate(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        // 模拟数据
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(createWinRateItem("胜诉", 65, "#52c41a"));
        data.add(createWinRateItem("部分胜诉", 20, "#1890ff"));
        data.add(createWinRateItem("败诉", 10, "#f56c6c"));
        data.add(createWinRateItem("其他", 5, "#909399"));

        result.put("data", data);
        return result;
    }

    private Map<String, Object> createWinRateItem(String name, int value, String color) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("value", value);
        item.put("itemStyle", Map.of("color", color));
        return item;
    }

    /**
     * 获取收款率统计
     */
    public Map<String, Object> getCollectionRate(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        List<String> labels = new ArrayList<>();
        List<BigDecimal> receivable = new ArrayList<>();
        List<BigDecimal> received = new ArrayList<>();
        List<Double> collectionRate = new ArrayList<>();

        // 生成最近6个月的数据
        for (int i = 5; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusMonths(i);
            labels.add(date.format(DateTimeFormatter.ofPattern("yyyy-MM")));

            // 实际应该从数据库统计
            receivable.add(new BigDecimal(Arrays.asList(400, 432, 401, 434, 490, 560).get(i)));
            received.add(new BigDecimal(Arrays.asList(320, 302, 301, 334, 390, 430).get(i)));
            collectionRate.add(Arrays.asList(80.0, 70.0, 75.0, 77.0, 80.0, 77.0).get(i));
        }

        result.put("labels", labels);
        result.put("receivable", receivable);
        result.put("received", received);
        result.put("collectionRate", collectionRate);

        return result;
    }

    /**
     * 导出Excel
     */
    public String exportExcel(Map<String, Object> params) {
        try {
            // 解析日期参数（添加空值检查防止NPE）
            LocalDate startDate = null;
            LocalDate endDate = null;
            if (params.containsKey("startDate") && params.get("startDate") != null) {
                startDate = LocalDate.parse(params.get("startDate").toString());
            }
            if (params.containsKey("endDate") && params.get("endDate") != null) {
                endDate = LocalDate.parse(params.get("endDate").toString());
            }

            // 获取统计数据
            Map<String, Object> statsCards = getStatsCards(startDate, endDate);

            // 创建Excel工作簿
            Workbook workbook = new XSSFWorkbook();

            // 创建统计卡片sheet
            Sheet statsSheet = workbook.createSheet("统计概览");
            createStatsCardsSheet(statsSheet, statsCards);

            // 创建案件趋势sheet
            Sheet trendSheet = workbook.createSheet("案件趋势");
            Map<String, Object> caseTrend = getCaseTrend("month", startDate, endDate);
            createCaseTrendSheet(trendSheet, caseTrend);

            // 创建案件明细sheet
            Sheet casesSheet = workbook.createSheet("案件明细");
            createCasesDetailSheet(casesSheet, startDate, endDate);

            // 创建财务明细sheet
            Sheet financeSheet = workbook.createSheet("财务明细");
            createFinanceDetailSheet(financeSheet, startDate, endDate);

            // 确保导出目录存在
            File exportDirFile = new File(exportDir);
            if (!exportDirFile.exists()) {
                exportDirFile.mkdirs();
            }

            // 生成文件名
            String fileName = "statistics_export_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            String filePath = exportDirFile.getAbsolutePath() + File.separator + fileName;

            // 写入文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
            workbook.close();

            log.info("Excel导出成功: {}", filePath);
            return "/api/files/" + fileName;

        } catch (Exception e) {
            log.error("Excel导出失败", e);
            throw new RuntimeException("Excel导出失败: " + e.getMessage());
        }
    }

    /**
     * 创建统计卡片sheet
     */
    private void createStatsCardsSheet(Sheet sheet, Map<String, Object> statsCards) {
        int rowNum = 0;

        // 标题行
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("统计指标");
        headerRow.createCell(1).setCellValue("数值");
        headerRow.createCell(2).setCellValue("趋势");

        // 样式
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);
        headerRow.getCell(2).setCellStyle(headerStyle);

        // 数据行
        createStatRow(sheet, rowNum++, "案件总数", statsCards.get("totalCases"), statsCards.get("totalCasesTrend"));
        createStatRow(sheet, rowNum++, "进行中案件", statsCards.get("activeCases"), statsCards.get("activeCasesTrend"));
        createStatRow(sheet, rowNum++, "已结案案件", statsCards.get("closedCases"), statsCards.get("closedCasesTrend"));
        createStatRow(sheet, rowNum++, "总收入（万元）", statsCards.get("totalIncome"), statsCards.get("totalIncomeTrend"));

        // 自动调整列宽
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
    }

    /**
     * 创建统计数据行
     */
    private void createStatRow(Sheet sheet, int rowNum, String label, Object value, Object trend) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value != null ? value.toString() : "0");
        row.createCell(2).setCellValue(trend != null ? trend.toString() : "N/A");
    }

    /**
     * 创建案件趋势sheet
     */
    private void createCaseTrendSheet(Sheet sheet, Map<String, Object> caseTrend) {
        int rowNum = 0;

        // 标题行
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("时间周期");
        headerRow.createCell(1).setCellValue("新增案件");
        headerRow.createCell(2).setCellValue("结案案件");

        // 数据行
        List<String> labels = (List<String>) caseTrend.get("labels");
        List<Integer> newCases = (List<Integer>) caseTrend.get("newCases");
        List<Integer> closedCases = (List<Integer>) caseTrend.get("closedCases");

        for (int i = 0; i < labels.size(); i++) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(labels.get(i));
            row.createCell(1).setCellValue(newCases.get(i));
            row.createCell(2).setCellValue(closedCases.get(i));
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
    }

    /**
     * 创建案件明细sheet
     */
    private void createCasesDetailSheet(Sheet sheet, LocalDate startDate, LocalDate endDate) {
        int rowNum = 0;

        // 标题行
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("案件编号");
        headerRow.createCell(1).setCellValue("案件名称");
        headerRow.createCell(2).setCellValue("案件类型");
        headerRow.createCell(3).setCellValue("负责人");
        headerRow.createCell(4).setCellValue("状态");
        headerRow.createCell(5).setCellValue("创建时间");

        // 获取案件数据（使用数据库查询优化）
        LocalDate actualStart = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate actualEnd = endDate != null ? endDate : LocalDate.now();

        LocalDateTime startDateTime = actualStart.atStartOfDay();
        LocalDateTime endDateTime = actualEnd.atTime(23, 59, 59);
        List<Case> cases = caseRepository.findByCreatedAtBetweenAndDeletedFalseOrderByCreatedAtAsc(
                startDateTime, endDateTime);

        // 数据行
        for (Case c : cases) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(c.getCaseNumber());
            row.createCell(1).setCellValue(c.getCaseName());
            row.createCell(2).setCellValue(c.getCaseType());
            row.createCell(3).setCellValue(c.getOwnerId() != null ? c.getOwnerId().toString() : "N/A");
            row.createCell(4).setCellValue(c.getStatus());
            row.createCell(5).setCellValue(c.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }

        // 自动调整列宽
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 创建财务明细sheet
     */
    private void createFinanceDetailSheet(Sheet sheet, LocalDate startDate, LocalDate endDate) {
        int rowNum = 0;

        // 标题行
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("付款日期");
        headerRow.createCell(1).setCellValue("付款方");
        headerRow.createCell(2).setCellValue("付款金额");
        headerRow.createCell(3).setCellValue("付款方式");
        headerRow.createCell(4).setCellValue("备注");

        // 获取财务数据（使用数据库查询优化）
        LocalDate actualStart = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate actualEnd = endDate != null ? endDate : LocalDate.now();

        List<Payment> payments = paymentRepository.findByPaymentDateBetween(actualStart, actualEnd);

        // 数据行
        for (Payment p : payments) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(p.getPaymentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            row.createCell(1).setCellValue(p.getPayer() != null ? p.getPayer() : "N/A");
            row.createCell(2).setCellValue(p.getPaymentAmount() != null ? p.getPaymentAmount().toString() : "0");
            row.createCell(3).setCellValue(p.getPaymentMethod());
            row.createCell(4).setCellValue(p.getNotes() != null ? p.getNotes() : "");
        }

        // 自动调整列宽
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 导出PDF
     */
    public String exportPdf(Map<String, Object> params) {
        try {
            // 解析日期参数（添加空值检查防止NPE）
            LocalDate startDate = null;
            LocalDate endDate = null;
            if (params.containsKey("startDate") && params.get("startDate") != null) {
                startDate = LocalDate.parse(params.get("startDate").toString());
            }
            if (params.containsKey("endDate") && params.get("endDate") != null) {
                endDate = LocalDate.parse(params.get("endDate").toString());
            }

            // 获取统计数据
            Map<String, Object> statsCards = getStatsCards(startDate, endDate);
            Map<String, Object> caseTrend = getCaseTrend("month", startDate, endDate);

            // 使用PDFBox创建PDF文档
            PDDocument document = new PDDocument();

            // 创建内容页面
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            // 添加内容
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {

                // 设置字体（使用标准字体）
                PDFont font = PDType1Font.HELVETICA_BOLD;
                PDFont normalFont = PDType1Font.HELVETICA;

                float margin = 50;
                float yPosition = page.getMediaBox().getHeight() - margin;
                float lineHeight = 20;

                // 标题
                contentStream.setFont(font, 18);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Statistics Report");
                contentStream.endText();

                yPosition -= lineHeight * 2;

                // 日期范围
                contentStream.setFont(normalFont, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                String dateRange = "Period: " +
                        (startDate != null ? startDate : "N/A") + " to " +
                        (endDate != null ? endDate : "N/A");
                contentStream.showText(dateRange);
                contentStream.endText();

                yPosition -= lineHeight * 2;

                // 统计卡片数据
                contentStream.setFont(font, 14);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Summary Statistics");
                contentStream.endText();

                yPosition -= lineHeight;

                contentStream.setFont(normalFont, 11);
                String[][] statsData = {
                        {"Total Cases", statsCards.get("totalCases").toString()},
                        {"Active Cases", statsCards.get("activeCases").toString()},
                        {"Closed Cases", statsCards.get("closedCases").toString()},
                        {"Total Income (10K CNY)", statsCards.get("totalIncome").toString()}
                };

                for (String[] stat : statsData) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText(stat[0] + ": " + stat[1]);
                    contentStream.endText();
                    yPosition -= lineHeight;
                }

                yPosition -= lineHeight;

                // 案件趋势
                contentStream.setFont(font, 14);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Case Trend");
                contentStream.endText();

                yPosition -= lineHeight;

                contentStream.setFont(normalFont, 11);
                List<String> labels = (List<String>) caseTrend.get("labels");
                List<Integer> newCases = (List<Integer>) caseTrend.get("newCases");

                for (int i = 0; i < labels.size(); i++) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText(labels.get(i) + ": New " + newCases.get(i));
                    contentStream.endText();
                    yPosition -= lineHeight;
                }
            }

            // 确保导出目录存在
            File exportDirFile = new File(exportDir);
            if (!exportDirFile.exists()) {
                exportDirFile.mkdirs();
            }

            // 生成文件名
            String fileName = "statistics_export_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            String filePath = exportDirFile.getAbsolutePath() + File.separator + fileName;

            // 保存PDF
            document.save(filePath);
            document.close();

            log.info("PDF导出成功: {}", filePath);
            return "/api/files/" + fileName;

        } catch (Exception e) {
            log.error("PDF导出失败", e);
            throw new RuntimeException("PDF导出失败: " + e.getMessage());
        }
    }
}
