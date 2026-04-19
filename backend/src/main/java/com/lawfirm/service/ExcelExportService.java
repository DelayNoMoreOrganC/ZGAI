package com.lawfirm.service;

import com.lawfirm.entity.CaseRecord;
import com.lawfirm.repository.CaseRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 导出服务（Excel + Word）
 * PRD要求（320行）：导出Word/Excel
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final CaseRecordRepository caseRecordRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 导出办案记录为Excel
     */
    public byte[] exportCaseRecords(Long caseId, String stage, String startDate, String endDate, String keyword) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("办案记录");

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setFont(createBoldFont(workbook));

            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"序号", "记录标题", "案件阶段", "记录内容", "工时(h)", "记录日期", "记录人"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 获取办案记录数据
            List<CaseRecord> records = caseRecordRepository.findByCaseIdAndDeletedFalseOrderByRecordDateDescCreatedAtDesc(caseId);

            // 填充数据
            int rowNum = 1;
            for (int i = 0; i < records.size(); i++) {
                CaseRecord record = records.get(i);

                // 应用筛选条件
                if (stage != null && !stage.isEmpty() && !stage.equals(record.getStage())) {
                    continue;
                }
                if (keyword != null && !keyword.isEmpty()) {
                    String title = record.getTitle() != null ? record.getTitle() : "";
                    String content = record.getContent() != null ? record.getContent() : "";
                    if (!title.contains(keyword) && !content.contains(keyword)) {
                        continue;
                    }
                }

                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(record.getTitle() != null ? record.getTitle() : "");
                row.createCell(2).setCellValue(record.getStage() != null ? record.getStage() : "");
                row.createCell(3).setCellValue(record.getContent() != null ? record.getContent() : "");
                row.createCell(4).setCellValue(record.getWorkHours() != null ? record.getWorkHours().doubleValue() : 0);
                row.createCell(5).setCellValue(record.getRecordDate() != null ?
                    record.getRecordDate().format(DATE_FORMATTER) : "");
                row.createCell(6).setCellValue(record.getCreatedBy() != null ? String.valueOf(record.getCreatedBy()) : "");
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 写入字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            log.info("办案记录Excel导出成功: 案件ID={}, 记录数={}", caseId, rowNum - 1);
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("导出办案记录Excel失败", e);
            throw new RuntimeException("导出办案记录Excel失败: " + e.getMessage());
        }
    }

    /**
     * 创建粗体字体
     */
    private Font createBoldFont(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        return font;
    }

    /**
     * 导出办案记录为Word
     * PRD要求（320行）：支持导出Word格式
     */
    public byte[] exportCaseRecordsToWord(Long caseId, String stage, String startDate, String endDate, String keyword) {
        try (XWPFDocument document = new XWPFDocument()) {

            // 添加标题
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText("办案记录");
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setFontFamily("宋体");

            // 获取办案记录数据
            List<CaseRecord> records = caseRecordRepository.findByCaseIdAndDeletedFalseOrderByRecordDateDescCreatedAtDesc(caseId);

            // 创建表格
            XWPFTable table = document.createTable(records.size() + 1, 7);
            CTTblPr tblPr = table.getCTTbl().getTblPr();
            if (tblPr == null) {
                tblPr = table.getCTTbl().addNewTblPr();
            }

            // 设置表格宽度
            CTTblWidth tblWidth = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
            tblWidth.setW(BigInteger.valueOf(9500));
            tblWidth.setType(STTblWidth.DXA);

            // 创建表头
            XWPFTableRow headerRow = table.getRow(0);
            String[] headers = {"序号", "记录标题", "案件阶段", "记录内容", "工时(h)", "记录日期", "记录人"};
            for (int i = 0; i < headers.length; i++) {
                XWPFParagraph cellPara = headerRow.getCell(i).addParagraph();
                XWPFRun run = cellPara.createRun();
                run.setText(headers[i]);
                run.setBold(true);
                cellPara.setAlignment(ParagraphAlignment.CENTER);
            }

            // 填充数据
            int actualRowNum = 1;
            for (int i = 0; i < records.size(); i++) {
                CaseRecord record = records.get(i);

                // 应用筛选条件
                if (stage != null && !stage.isEmpty() && !stage.equals(record.getStage())) {
                    continue;
                }
                if (keyword != null && !keyword.isEmpty()) {
                    String title = record.getTitle() != null ? record.getTitle() : "";
                    String content = record.getContent() != null ? record.getContent() : "";
                    if (!title.contains(keyword) && !content.contains(keyword)) {
                        continue;
                    }
                }

                XWPFTableRow row = table.createRow();

                row.getCell(0).setText(String.valueOf(actualRowNum));
                row.getCell(1).setText(record.getTitle() != null ? record.getTitle() : "");
                row.getCell(2).setText(record.getStage() != null ? record.getStage() : "");

                // 记录内容可能很长，需要处理换行
                String content = record.getContent() != null ? record.getContent() : "";
                row.getCell(3).setText(content.length() > 100 ? content.substring(0, 100) + "..." : content);

                row.getCell(4).setText(record.getWorkHours() != null ? record.getWorkHours().toString() : "0");
                row.getCell(5).setText(record.getRecordDate() != null ?
                    record.getRecordDate().format(DATE_FORMATTER) : "");
                row.getCell(6).setText(record.getCreatedBy() != null ? String.valueOf(record.getCreatedBy()) : "");

                actualRowNum++;
            }

            // 写入字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);

            log.info("办案记录Word导出成功: 案件ID={}, 记录数={}", caseId, actualRowNum - 1);
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("导出办案记录Word失败", e);
            throw new RuntimeException("导出办案记录Word失败: " + e.getMessage());
        }
    }
}
