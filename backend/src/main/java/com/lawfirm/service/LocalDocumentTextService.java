package com.lawfirm.service;

import com.lawfirm.dto.OcrHealthDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
public class LocalDocumentTextService {

    @Value("${ai.ocr.tesseract-command:tesseract}")
    private String tesseractCommand = "tesseract";

    @Value("${ai.ocr.pdftoppm-command:pdftoppm}")
    private String pdftoppmCommand = "pdftoppm";

    @Value("${ai.ocr.language:chi_sim+eng}")
    private String ocrLanguage = "chi_sim+eng";

    @Value("${ai.ocr.max-pdf-pages:100}")
    private int maxPdfPages = 100;

    public String extract(Path file, String originalName, String mimeType) throws Exception {
        String extension = extension(originalName);
        if ("pdf".equals(extension) || "application/pdf".equals(mimeType)) {
            String text = extractPdfText(file);
            return text.length() >= 20 ? text : ocrPdf(file);
        }
        if ("docx".equals(extension)) return extractDocx(file);
        if ("txt".equals(extension) || "md".equals(extension)) {
            return Files.readString(file, StandardCharsets.UTF_8);
        }
        if ("png".equals(extension) || "jpg".equals(extension) || "jpeg".equals(extension)
                || (mimeType != null && mimeType.startsWith("image/"))) {
            return ocrImage(file);
        }
        throw new IllegalArgumentException("暂不支持该文件格式");
    }

    public OcrHealthDTO getHealth() {
        ProbeResult tesseract = probe(tesseractCommand, "--list-langs");
        boolean languageReady = tesseract.success && hasConfiguredLanguages(tesseract.output);
        ProbeResult pdftoppm = probe(pdftoppmCommand, "-v");
        boolean imageReady = tesseract.success && languageReady;
        boolean scannedPdfReady = imageReady && pdftoppm.success;
        String message;
        if (scannedPdfReady) {
            message = "本地图片与扫描PDF OCR可用";
        } else if (!tesseract.success) {
            message = "Tesseract不可用，文字型PDF、DOCX和文本文件仍可提取";
        } else if (!languageReady) {
            message = "OCR语言包不完整，文字型PDF、DOCX和文本文件仍可提取";
        } else {
            message = "pdftoppm不可用，图片OCR可用，扫描PDF暂不可用";
        }
        return OcrHealthDTO.builder()
                .status(scannedPdfReady ? "READY" : "DEGRADED")
                .textDocumentExtractionReady(true)
                .imageOcrReady(imageReady)
                .scannedPdfOcrReady(scannedPdfReady)
                .language(ocrLanguage)
                .message(message)
                .build();
    }

    private String extractPdfText(Path file) throws Exception {
        try (PDDocument document = PDDocument.load(file.toFile())) {
            if (document.getNumberOfPages() > maxPdfPages) {
                throw new IllegalArgumentException("扫描PDF不能超过" + maxPdfPages + "页");
            }
            return new PDFTextStripper().getText(document).trim();
        }
    }

    private String extractDocx(Path file) throws Exception {
        try (InputStream input = Files.newInputStream(file); XWPFDocument document = new XWPFDocument(input)) {
            return document.getParagraphs().stream().map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n")).trim();
        }
    }

    private String ocrPdf(Path file) throws Exception {
        Path workDir = Files.createTempDirectory("zgai-ocr-");
        try {
            Path prefix = workDir.resolve("page");
            run(new ProcessBuilder(pdftoppmCommand, "-png", "-r", "200", file.toString(), prefix.toString()), 180);
            StringBuilder text = new StringBuilder();
            try (java.util.stream.Stream<Path> pages = Files.list(workDir)) {
                for (Path page : pages.filter(path -> path.getFileName().toString().endsWith(".png"))
                        .sorted().collect(Collectors.toList())) {
                    text.append(ocrImage(page)).append('\n');
                }
            }
            return text.toString().trim();
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.walk(workDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) { }
                });
            }
        }
    }

    private String ocrImage(Path file) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(tesseractCommand, file.toString(), "stdout",
                "-l", ocrLanguage, "--psm", "6");
        return run(builder, 120).trim();
    }

    private String run(ProcessBuilder builder, int timeoutSeconds) throws Exception {
        builder.redirectErrorStream(true);
        Process process = builder.start();
        CompletableFuture<byte[]> outputFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return process.getInputStream().readAllBytes();
            } catch (Exception e) {
                throw new IllegalStateException("读取本地OCR输出失败", e);
            }
        });
        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalArgumentException("本地OCR处理超时");
            }
            byte[] output = outputFuture.get(5, TimeUnit.SECONDS);
            if (process.exitValue() != 0) {
                throw new IllegalArgumentException("本地OCR处理失败");
            }
            return new String(output, StandardCharsets.UTF_8);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IllegalArgumentException("本地OCR处理被中断", e);
        } catch (ExecutionException | TimeoutException e) {
            process.destroyForcibly();
            throw new IllegalArgumentException("本地OCR输出读取失败", e);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    private ProbeResult probe(String command, String argument) {
        try {
            String output = run(new ProcessBuilder(command, argument), 5);
            return new ProbeResult(true, output);
        } catch (Exception e) {
            return new ProbeResult(false, "");
        }
    }

    private boolean hasConfiguredLanguages(String output) {
        Set<String> available = output.lines()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
        return Arrays.stream(ocrLanguage.split("\\+"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .allMatch(available::contains);
    }

    private String extension(String name) {
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static class ProbeResult {
        private final boolean success;
        private final String output;

        private ProbeResult(boolean success, String output) {
            this.success = success;
            this.output = output;
        }
    }
}
