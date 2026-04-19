package com.lawfirm.service;

import com.lawfirm.entity.Case;
import com.lawfirm.entity.CaseDocument;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.CaseDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 一键归档PDF服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchivePdfService {

    private final CaseRepository caseRepository;
    private final CaseDocumentRepository caseDocumentRepository;

    @Value("${file.upload-path:D:/ZGAI/uploads/}")
    private String uploadPath;

    /**
     * 生成归档PDF
     */
    @Transactional(readOnly = true)
    public String generateArchivePdf(Long caseId) {
        PDDocument document = null;
        try {
            Case caseEntity = caseRepository.findById(caseId)
                    .orElseThrow(() -> new RuntimeException("案件不存在"));

            List<CaseDocument> documents = caseDocumentRepository.findByCaseIdOrderByCreatedAtDesc(caseId);

            // 生成PDF文件名
            String fileName = "归档_" + caseEntity.getCaseName() + "_" +
                    java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

            // 生成PDF文件路径
            String pdfPath = uploadPath + "archive/" + fileName;

            // 创建目录
            Path outputPath = Paths.get(pdfPath);
            Files.createDirectories(outputPath.getParent());

            // 创建PDF文档
            document = new PDDocument();

            // 加载中文字体 (使用 Windows 系统字体)
            PDFont font;
            PDFont boldFont;
            try {
                // 尝试加载系统中文字体 (优先使用 TTF 文件)
                String[] fontPaths = {
                    "C:/Windows/Fonts/simhei.ttf",    // 黑体
                    "C:/Windows/Fonts/simfang.ttf",   // 仿宋
                    "C:/Windows/Fonts/simkai.ttf",    // 楷体
                    "C:/Windows/Fonts/SimsunExtG.ttf", // 宋体扩展
                    "/System/Library/Fonts/STHeiti Medium.ttf",  // Mac 黑体
                    "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc"  // Linux
                };

                font = null;
                for (String fontPath : fontPaths) {
                    File fontFile = new File(fontPath);
                    if (fontFile.exists()) {
                        try {
                            font = PDType0Font.load(document, fontFile);
                            log.info("成功加载中文字体: {}", fontPath);
                            break;
                        } catch (Exception e) {
                            log.debug("无法加载字体 {}: {}", fontPath, e.getMessage());
                        }
                    }
                }

                if (font == null) {
                    // 如果所有中文字体都加载失败，使用 Helvetica
                    log.warn("无法加载中文字体，使用 Helvetica 作为后备");
                    font = PDType1Font.HELVETICA;
                }

                boldFont = font; // 使用同一字体
            } catch (Exception e) {
                log.warn("加载中文字体失败，使用 Helvetica: {}", e.getMessage());
                font = PDType1Font.HELVETICA;
                boldFont = PDType1Font.HELVETICA_BOLD;
            }

            // 1. 生成封面
            PDPage coverPage = new PDPage(PDRectangle.A4);
            document.addPage(coverPage);
            createCoverPage(document, coverPage, caseEntity, font, boldFont);

            // 2. 生成目录页
            PDPage tocPage = new PDPage(PDRectangle.A4);
            document.addPage(tocPage);
            int tocStartPageNum = document.getPages().getCount() - 1;
            createTableOfContents(document, tocPage, documents, font, boldFont);

            // 3. 拼接文档并添加书签
            PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
            if (outline == null) {
                outline = new PDDocumentOutline();
                document.getDocumentCatalog().setDocumentOutline(outline);
            }

            // 添加封面书签
            PDPageDestination coverDest = new PDPageFitDestination();
            coverDest.setPage(document.getPage(0));
            PDOutlineItem coverBookmark = new PDOutlineItem();
            coverBookmark.setTitle("封面");
            coverBookmark.setDestination(coverDest);
            outline.addFirst(coverBookmark);

            // 添加目录书签
            PDPageDestination tocDest = new PDPageFitDestination();
            tocDest.setPage(document.getPage(tocStartPageNum));
            PDOutlineItem tocBookmark = new PDOutlineItem();
            tocBookmark.setTitle("目录");
            tocBookmark.setDestination(tocDest);
            outline.addLast(tocBookmark);

            // 添加文档书签和内容
            int currentPageNum = tocStartPageNum + 1;
            for (CaseDocument doc : documents) {
                try {
                    File docFile = new File(uploadPath + doc.getFilePath());
                    if (docFile.exists() && docFile.getName().toLowerCase().endsWith(".pdf")) {
                        // 合并PDF文档
                        PDDocument docToMerge = PDDocument.load(docFile);
                        for (PDPage page : docToMerge.getPages()) {
                            document.importPage(page);
                        }

                        // 添加书签
                        PDPageDestination docDest = new PDPageFitDestination();
                        docDest.setPage(document.getPage(currentPageNum));
                        PDOutlineItem docBookmark = new PDOutlineItem();
                        docBookmark.setTitle(doc.getDocumentName());
                        docBookmark.setDestination(docDest);
                        outline.addLast(docBookmark);

                        currentPageNum += docToMerge.getNumberOfPages();
                        docToMerge.close();
                    }
                } catch (Exception e) {
                    log.warn("无法合并文档: {}", doc.getDocumentName(), e);
                }
            }

            // 保存PDF
            document.save(pdfPath);
            document.close();

            log.info("归档PDF生成成功: {}", pdfPath);
            return "/api/archive/" + fileName;

        } catch (Exception e) {
            log.error("生成归档PDF失败", e);
            if (document != null) {
                try {
                    document.close();
                } catch (Exception ex) {
                    log.error("关闭PDF文档失败", ex);
                }
            }
            throw new RuntimeException("生成归档PDF失败: " + e.getMessage());
        }
    }

    /**
     * 创建封面页
     */
    private void createCoverPage(PDDocument document, PDPage page, Case caseEntity,
                                  PDFont font, PDFont boldFont) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        PDRectangle pageSize = page.getMediaBox();
        float pageWidth = pageSize.getWidth();
        float pageHeight = pageSize.getHeight();
        float margin = 50;
        float yPosition = pageHeight - 100;

        // 设置字体
        contentStream.setFont(boldFont, 24);
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("Case Archive");
        contentStream.endText();

        yPosition -= 60;
        contentStream.setFont(boldFont, 18);
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("Case Name: " + (caseEntity.getCaseName() != null ? caseEntity.getCaseName() : ""));
        contentStream.endText();

        yPosition -= 40;
        contentStream.setFont(font, 14);
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("Case Number: " + (caseEntity.getCaseNumber() != null ? caseEntity.getCaseNumber() : ""));
        contentStream.endText();

        yPosition -= 30;
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("Case Type: " + (caseEntity.getCaseType() != null ? caseEntity.getCaseType() : ""));
        contentStream.endText();

        yPosition -= 30;
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("Case Reason: " + (caseEntity.getCaseReason() != null ? caseEntity.getCaseReason() : ""));
        contentStream.endText();

        yPosition -= 30;
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("Current Stage: " + (caseEntity.getCurrentStage() != null ? caseEntity.getCurrentStage() : ""));
        contentStream.endText();

        yPosition -= 30;
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("Archive Date: " + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        contentStream.endText();

        contentStream.close();
    }

    /**
     * 创建目录页
     */
    private void createTableOfContents(PDDocument document, PDPage page,
                                       List<CaseDocument> documents,
                                       PDFont font, PDFont boldFont) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        PDRectangle pageSize = page.getMediaBox();
        float pageWidth = pageSize.getWidth();
        float pageHeight = pageSize.getHeight();
        float margin = 50;
        float yPosition = pageHeight - 80;

        // 标题
        contentStream.setFont(boldFont, 18);
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("Table of Contents");
        contentStream.endText();

        yPosition -= 50;
        contentStream.setFont(font, 12);

        // 文档列表
        if (documents != null && !documents.isEmpty()) {
            for (int i = 0; i < documents.size(); i++) {
                CaseDocument doc = documents.get(i);
                yPosition -= 25;

                if (yPosition < margin) {
                    // 需要新页面
                    contentStream.close();
                    PDPage newPage = new PDPage(PDRectangle.A4);
                    document.addPage(newPage);
                    contentStream = new PDPageContentStream(document, newPage);
                    yPosition = pageHeight - 80;
                }

                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                String docName = (i + 1) + ". " + (doc.getDocumentName() != null ? doc.getDocumentName() : "");
                // 截断过长的文档名
                if (docName.length() > 50) {
                    docName = docName.substring(0, 47) + "...";
                }
                contentStream.showText(docName);
                contentStream.endText();

                // 显示上传日期
                yPosition -= 15;
                contentStream.beginText();
                contentStream.newLineAtOffset(margin + 20, yPosition);
                String uploadDate = doc.getCreatedAt() != null ?
                    doc.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";
                contentStream.showText("Uploaded: " + uploadDate);
                contentStream.endText();

                yPosition -= 10;
            }
        } else {
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("No documents");
            contentStream.endText();
        }

        contentStream.close();
    }

    /**
     * 下载归档PDF
     */
    @Transactional(readOnly = true)
    public void downloadArchivePdf(Long caseId, HttpServletResponse response) {
        try {
            Case caseEntity = caseRepository.findById(caseId)
                    .orElseThrow(() -> new RuntimeException("案件不存在"));

            String fileName = "归档_" + caseEntity.getCaseName() + "_" +
                    java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

            String pdfPath = uploadPath + "archive/" + fileName;

            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists()) {
                // 如果文件不存在，先生成
                generateArchivePdf(caseId);
            }

            // 设置响应头
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=" +
                    new String(fileName.getBytes("UTF-8"), "ISO-8859-1"));

            // 输出文件
            try (InputStream inputStream = new FileInputStream(pdfFile);
                 OutputStream outputStream = response.getOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }

        } catch (Exception e) {
            log.error("下载归档PDF失败", e);
            throw new RuntimeException("下载归档PDF失败: " + e.getMessage());
        }
    }
}
