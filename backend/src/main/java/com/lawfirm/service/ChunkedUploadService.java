package com.lawfirm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;
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

    @Value("${upload.max.file-size:52428800}") // 50MB per file
    private long maxFileSize;

    // 存储上传进度信息：uploadId -> UploadProgress
    private final Map<String, UploadProgress> uploadProgressMap = new ConcurrentHashMap<>();

    /**
     * 初始化分片上传
     */
    public String initChunkedUpload(String fileName, long fileSize, String mimeType, Long ownerUserId) {
        if (ownerUserId == null) {
            throw new IllegalArgumentException("上传用户不能为空");
        }
        if (fileSize <= 0 || fileSize > maxFileSize) {
            throw new IllegalArgumentException("文件大小必须大于0且不能超过50MB");
        }
        if (mimeType == null || mimeType.trim().isEmpty() || mimeType.length() > 150) {
            throw new IllegalArgumentException("文件类型不能为空或过长");
        }
        String safeFileName = sanitizeFileName(fileName);
        String uploadId = UUID.randomUUID().toString();

        UploadProgress progress = new UploadProgress();
        progress.setUploadId(uploadId);
        progress.setFileName(safeFileName);
        progress.setFileSize(fileSize);
        progress.setMimeType(mimeType);
        progress.setOwnerUserId(ownerUserId);
        progress.setUploadedChunks(0);
        progress.setTotalChunks((int) Math.ceil((double) fileSize / chunkSize));
        progress.setStatus("INITIALIZED");

        uploadProgressMap.put(uploadId, progress);

        // 创建临时目录
        try {
            Path tempDir = sessionDirectory(uploadId);
            Files.createDirectories(tempDir);
        } catch (IOException e) {
            log.error("创建上传临时目录失败", e);
            throw new RuntimeException("创建上传临时目录失败");
        }

        log.info("初始化分片上传: uploadId={}, fileName={}, fileSize={}, totalChunks={}",
                uploadId, safeFileName, fileSize, progress.getTotalChunks());

        return uploadId;
    }

    /**
     * 上传分片
     */
    public synchronized UploadProgress uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk,
                                                    Long userId, boolean privileged) {
        UploadProgress progress = requireOwnedProgress(uploadId, userId, privileged);

        if (progress.getStatus().equals("COMPLETED")) {
            throw new IllegalArgumentException("上传已完成");
        }
        if (chunkIndex < 0 || chunkIndex >= progress.getTotalChunks()) {
            throw new IllegalArgumentException("分片序号超出范围");
        }
        if (chunk == null || chunk.isEmpty() || chunk.getSize() > chunkSize) {
            throw new IllegalArgumentException("分片不能为空且不能超过5MB");
        }

        try {
            Path chunkPath = sessionDirectory(uploadId).resolve("chunk_" + chunkIndex).normalize();
            long previousSize = Files.exists(chunkPath) ? Files.size(chunkPath) : 0L;
            long nextUploadedBytes = progress.getUploadedBytes() - previousSize + chunk.getSize();
            if (nextUploadedBytes > progress.getFileSize()) {
                throw new IllegalArgumentException("已上传分片总大小超过声明的文件大小");
            }
            chunk.transferTo(chunkPath.toFile());

            progress.setUploadedBytes(nextUploadedBytes);
            progress.getUploadedChunkIndexes().add(chunkIndex);
            progress.setUploadedChunks(progress.getUploadedChunkIndexes().size());
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
            throw new RuntimeException("保存分片失败");
        }
    }

    /**
     * 合并分片为完整文件
     */
    public String mergeChunks(String uploadId, Long userId, boolean privileged) {
        UploadProgress progress = requireOwnedProgress(uploadId, userId, privileged);
        if (progress.getUploadedChunkIndexes().size() != progress.getTotalChunks()) {
            throw new IllegalArgumentException("分片尚未全部上传");
        }

        try {
            // 创建最终文件路径
            String fileExtension = getFileExtension(progress.getFileName());
            String finalFileName = uploadId + "_" + System.currentTimeMillis() + fileExtension;
            Path uploadRoot = uploadRoot();
            Path finalPath = uploadRoot.resolve(finalFileName).normalize();
            if (!finalPath.startsWith(uploadRoot)) {
                throw new IllegalArgumentException("文件名不合法");
            }

            Files.createDirectories(finalPath.getParent());

            // 合并所有分片
            try (FileOutputStream fos = new FileOutputStream(finalPath.toFile())) {
                for (int i = 0; i < progress.getTotalChunks(); i++) {
                    Path chunkPath = sessionDirectory(uploadId).resolve("chunk_" + i).normalize();
                    if (!Files.exists(chunkPath)) {
                        throw new IllegalArgumentException("分片文件缺失: chunk_" + i);
                    }

                    Files.copy(chunkPath, fos);
                    Files.delete(chunkPath); // 删除已合并的分片
                }
            }

            // 删除临时目录
            if (Files.size(finalPath) != progress.getFileSize()) {
                Files.deleteIfExists(finalPath);
                throw new IllegalArgumentException("合并后文件大小与声明不一致");
            }

            Path tempDir = sessionDirectory(uploadId);
            Files.deleteIfExists(tempDir);

            // 更新状态
            progress.setStatus("COMPLETED");
            progress.setFinalPath(finalPath.toString());

            log.info("合并分片完成: uploadId={}, finalPath={}", uploadId, finalPath);

            return finalPath.toString();

        } catch (IOException e) {
            log.error("合并分片失败: uploadId={}", uploadId, e);
            throw new RuntimeException("合并分片失败");
        }
    }

    /**
     * 获取上传进度
     */
    public UploadProgress getUploadProgress(String uploadId, Long userId, boolean privileged) {
        return requireOwnedProgress(uploadId, userId, privileged);
    }

    /**
     * 取消上传
     */
    public void cancelUpload(String uploadId, Long userId, boolean privileged) {
        requireOwnedProgress(uploadId, userId, privileged);
        cancelUploadInternal(uploadId);
    }

    private void cancelUploadInternal(String uploadId) {
        UploadProgress progress = uploadProgressMap.get(uploadId);
        if (progress == null) {
            return;
        }

        try {
            Path tempDir = sessionDirectory(uploadId);
            if (Files.exists(tempDir)) {
                try (java.util.stream.Stream<Path> paths = Files.walk(tempDir)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("删除临时文件失败: {}", path, e);
                        }
                    });
                }
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
                cancelUploadInternal(entry.getKey());
                return true;
            }
            return false;
        });

        log.info("清理过期上传会话完成");
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex <= 0) {
            return "";
        }
        String extension = fileName.substring(lastDotIndex);
        return extension.matches("\\.[A-Za-z0-9]{1,10}") ? extension : "";
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        Path namePath = Paths.get(fileName).getFileName();
        String safeName = namePath == null ? "" : namePath.toString().trim();
        if (safeName.isEmpty() || safeName.length() > 255 || ".".equals(safeName) || "..".equals(safeName)) {
            throw new IllegalArgumentException("文件名不合法");
        }
        return safeName;
    }

    private UploadProgress requireOwnedProgress(String uploadId, Long userId, boolean privileged) {
        UploadProgress progress = uploadProgressMap.get(uploadId);
        if (progress == null) {
            throw new IllegalArgumentException("上传会话不存在");
        }
        if (!privileged && !progress.getOwnerUserId().equals(userId)) {
            throw new AccessDeniedException("无权操作该上传会话");
        }
        return progress;
    }

    private Path tempRoot() {
        return Paths.get(uploadTempDir).toAbsolutePath().normalize();
    }

    private Path sessionDirectory(String uploadId) {
        Path root = tempRoot();
        Path directory = root.resolve(uploadId).normalize();
        if (!directory.startsWith(root)) {
            throw new IllegalArgumentException("上传会话标识不合法");
        }
        return directory;
    }

    private Path uploadRoot() throws IOException {
        Path root = tempRoot().resolve("uploads").normalize();
        Files.createDirectories(root);
        return root;
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
        private Long ownerUserId;
        private final Set<Integer> uploadedChunkIndexes = new HashSet<>();
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
        public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

        public String getUploadId() { return uploadId; }
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public String getMimeType() { return mimeType; }
        public int getUploadedChunks() { return uploadedChunks; }
        public int getTotalChunks() { return totalChunks; }
        public long getUploadedBytes() { return uploadedBytes; }
        public String getStatus() { return status; }
        @JsonIgnore
        public String getFinalPath() { return finalPath; }
        @JsonIgnore
        public Long getOwnerUserId() { return ownerUserId; }
        @JsonIgnore
        public Set<Integer> getUploadedChunkIndexes() { return uploadedChunkIndexes; }
        public long getCreatedAt() { return createdAt; }

        public double getProgress() {
            return totalChunks > 0 ? (double) uploadedChunks / totalChunks : 0;
        }
    }
}
