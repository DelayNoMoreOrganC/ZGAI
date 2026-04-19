package com.lawfirm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分片上传服务
 * PRD要求（576行）：文件上传 ≤50MB，支持断点续传
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkedUploadService {

    @Value("${upload.temp.dir:/tmp/uploads}")
    private String uploadTempDir;

    @Value("${upload.chunk.size:5242880}") // 5MB per chunk
    private long chunkSize;

    // 存储上传进度信息：uploadId -> UploadProgress
    private final Map<String, UploadProgress> uploadProgressMap = new ConcurrentHashMap<>();

    /**
     * 初始化分片上传
     */
    public String initChunkedUpload(String fileName, long fileSize, String mimeType) {
        String uploadId = UUID.randomUUID().toString();

        UploadProgress progress = new UploadProgress();
        progress.setUploadId(uploadId);
        progress.setFileName(fileName);
        progress.setFileSize(fileSize);
        progress.setMimeType(mimeType);
        progress.setUploadedChunks(0);
        progress.setTotalChunks((int) Math.ceil((double) fileSize / chunkSize));
        progress.setStatus("INITIALIZED");

        uploadProgressMap.put(uploadId, progress);

        // 创建临时目录
        try {
            Path tempDir = Paths.get(uploadTempDir, uploadId);
            Files.createDirectories(tempDir);
        } catch (IOException e) {
            log.error("创建上传临时目录失败", e);
            throw new RuntimeException("创建上传临时目录失败: " + e.getMessage());
        }

        log.info("初始化分片上传: uploadId={}, fileName={}, fileSize={}, totalChunks={}",
                uploadId, fileName, fileSize, progress.getTotalChunks());

        return uploadId;
    }

    /**
     * 上传分片
     */
    public synchronized UploadProgress uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk) {
        UploadProgress progress = uploadProgressMap.get(uploadId);
        if (progress == null) {
            throw new RuntimeException("上传会话不存在: " + uploadId);
        }

        if (progress.getStatus().equals("COMPLETED")) {
            throw new RuntimeException("上传已完成: " + uploadId);
        }

        try {
            // 保存分片到临时文件
            Path chunkPath = Paths.get(uploadTempDir, uploadId, "chunk_" + chunkIndex);
            chunk.transferTo(chunkPath.toFile());

            // 更新进度
            progress.setUploadedBytes(progress.getUploadedBytes() + chunk.getSize());
            progress.setUploadedChunks(progress.getUploadedChunks() + 1);
            progress.setStatus("UPLOADING");

            log.info("上传分片: uploadId={}, chunkIndex={}, progress={}/{}",
                    uploadId, chunkIndex, progress.getUploadedChunks(), progress.getTotalChunks());

            // 检查是否所有分片都已上传
            if (progress.getUploadedChunks() >= progress.getTotalChunks()) {
                progress.setStatus("READY_TO_MERGE");
            }

            return progress;

        } catch (IOException e) {
            log.error("保存分片失败: uploadId={}, chunkIndex={}", uploadId, chunkIndex, e);
            throw new RuntimeException("保存分片失败: " + e.getMessage());
        }
    }

    /**
     * 合并分片为完整文件
     */
    public String mergeChunks(String uploadId) {
        UploadProgress progress = uploadProgressMap.get(uploadId);
        if (progress == null) {
            throw new RuntimeException("上传会话不存在: " + uploadId);
        }

        try {
            // 创建最终文件路径
            String fileExtension = getFileExtension(progress.getFileName());
            String finalFileName = uploadId + "_" + System.currentTimeMillis() + fileExtension;
            Path finalPath = Paths.get(uploadTempDir, "uploads", finalFileName);

            Files.createDirectories(finalPath.getParent());

            // 合并所有分片
            try (FileOutputStream fos = new FileOutputStream(finalPath.toFile())) {
                for (int i = 0; i < progress.getTotalChunks(); i++) {
                    Path chunkPath = Paths.get(uploadTempDir, uploadId, "chunk_" + i);
                    if (!Files.exists(chunkPath)) {
                        throw new RuntimeException("分片文件缺失: chunk_" + i);
                    }

                    Files.copy(chunkPath, fos);
                    Files.delete(chunkPath); // 删除已合并的分片
                }
            }

            // 删除临时目录
            Path tempDir = Paths.get(uploadTempDir, uploadId);
            Files.deleteIfExists(tempDir);

            // 更新状态
            progress.setStatus("COMPLETED");
            progress.setFinalPath(finalPath.toString());

            log.info("合并分片完成: uploadId={}, finalPath={}", uploadId, finalPath);

            return finalPath.toString();

        } catch (IOException e) {
            log.error("合并分片失败: uploadId={}", uploadId, e);
            throw new RuntimeException("合并分片失败: " + e.getMessage());
        }
    }

    /**
     * 获取上传进度
     */
    public UploadProgress getUploadProgress(String uploadId) {
        UploadProgress progress = uploadProgressMap.get(uploadId);
        if (progress == null) {
            throw new RuntimeException("上传会话不存在: " + uploadId);
        }
        return progress;
    }

    /**
     * 取消上传
     */
    public void cancelUpload(String uploadId) {
        UploadProgress progress = uploadProgressMap.get(uploadId);
        if (progress == null) {
            return;
        }

        try {
            // 删除临时文件
            Path tempDir = Paths.get(uploadTempDir, uploadId);
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                    .filter(path -> !path.equals(tempDir))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("删除临时文件失败: {}", path, e);
                        }
                    });
                Files.deleteIfExists(tempDir);
            }

            uploadProgressMap.remove(uploadId);
            log.info("取消上传: uploadId={}", uploadId);

        } catch (IOException e) {
            log.error("取消上传失败: uploadId={}", uploadId, e);
        }
    }

    /**
     * 清理过期的上传会话（超过24小时）
     */
    public void cleanupExpiredSessions() {
        long expireTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);

        uploadProgressMap.entrySet().removeIf(entry -> {
            UploadProgress progress = entry.getValue();
            if (progress.getCreatedAt() < expireTime) {
                cancelUpload(entry.getKey());
                return true;
            }
            return false;
        });

        log.info("清理过期上传会话完成");
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : "";
    }

    /**
     * 上传进度信息
     */
    public static class UploadProgress {
        private String uploadId;
        private String fileName;
        private long fileSize;
        private String mimeType;
        private int uploadedChunks;
        private int totalChunks;
        private long uploadedBytes;
        private String status; // INITIALIZED, UPLOADING, READY_TO_MERGE, COMPLETED, FAILED
        private String finalPath;
        private long createdAt = System.currentTimeMillis();

        // Getters and Setters
        public void setUploadId(String uploadId) { this.uploadId = uploadId; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public void setUploadedChunks(int uploadedChunks) { this.uploadedChunks = uploadedChunks; }
        public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
        public void setUploadedBytes(long uploadedBytes) { this.uploadedBytes = uploadedBytes; }
        public void setStatus(String status) { this.status = status; }
        public void setFinalPath(String finalPath) { this.finalPath = finalPath; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

        public String getUploadId() { return uploadId; }
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public String getMimeType() { return mimeType; }
        public int getUploadedChunks() { return uploadedChunks; }
        public int getTotalChunks() { return totalChunks; }
        public long getUploadedBytes() { return uploadedBytes; }
        public String getStatus() { return status; }
        public String getFinalPath() { return finalPath; }
        public long getCreatedAt() { return createdAt; }

        public double getProgress() {
            return totalChunks > 0 ? (double) uploadedChunks / totalChunks : 0;
        }
    }
}