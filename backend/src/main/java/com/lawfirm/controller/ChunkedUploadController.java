package com.lawfirm.controller;

import com.lawfirm.service.ChunkedUploadService;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分片上传控制器
 * PRD要求（576行）：文件上传 ≤50MB，支持断点续传
 */
@Slf4j
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class ChunkedUploadController {

    private final ChunkedUploadService chunkedUploadService;
    private final SecurityUtils securityUtils;

    /**
     * 初始化分片上传
     * POST /api/upload/init
     */
    @PostMapping("/init")
    @PreAuthorize("hasAuthority('DOCUMENT_EDIT')")
    public Result<String> initChunkedUpload(
            @RequestParam String fileName,
            @RequestParam long fileSize,
            @RequestParam String mimeType) {
        try {
            String uploadId = chunkedUploadService.initChunkedUpload(
                    fileName, fileSize, mimeType, securityUtils.getCurrentUserId());
            return Result.success(uploadId);
        } catch (IllegalArgumentException e) {
            return Result.validationError(e.getMessage());
        } catch (Exception e) {
            log.error("初始化分片上传失败", e);
            return Result.error("初始化分片上传失败");
        }
    }

    /**
     * 上传分片
     * POST /api/upload/chunk
     */
    @PostMapping("/chunk")
    @PreAuthorize("hasAuthority('DOCUMENT_EDIT')")
    public Result<ChunkedUploadService.UploadProgress> uploadChunk(
            @RequestParam String uploadId,
            @RequestParam int chunkIndex,
            @RequestParam MultipartFile chunk) {
        try {
            ChunkedUploadService.UploadProgress progress = chunkedUploadService.uploadChunk(
                    uploadId, chunkIndex, chunk, securityUtils.getCurrentUserId(), securityUtils.isAdmin());
            return Result.success(progress);
        } catch (AccessDeniedException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            return Result.validationError(e.getMessage());
        } catch (Exception e) {
            log.error("上传分片失败: uploadId={}, chunkIndex={}", uploadId, chunkIndex, e);
            return Result.error("上传分片失败");
        }
    }

    /**
     * 合并分片
     * POST /api/upload/merge
     */
    @PostMapping("/merge")
    @PreAuthorize("hasAuthority('DOCUMENT_EDIT')")
    public Result<String> mergeChunks(@RequestParam String uploadId) {
        try {
            chunkedUploadService.mergeChunks(
                    uploadId, securityUtils.getCurrentUserId(), securityUtils.isAdmin());
            return Result.success("分片合并完成", uploadId);
        } catch (AccessDeniedException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            return Result.validationError(e.getMessage());
        } catch (Exception e) {
            log.error("合并分片失败: uploadId={}", uploadId, e);
            return Result.error("合并分片失败");
        }
    }

    /**
     * 获取上传进度
     * GET /api/upload/progress/{uploadId}
     */
    @GetMapping("/progress/{uploadId}")
    @PreAuthorize("hasAuthority('DOCUMENT_EDIT')")
    public Result<ChunkedUploadService.UploadProgress> getUploadProgress(@PathVariable String uploadId) {
        try {
            ChunkedUploadService.UploadProgress progress = chunkedUploadService.getUploadProgress(
                    uploadId, securityUtils.getCurrentUserId(), securityUtils.isAdmin());
            return Result.success(progress);
        } catch (AccessDeniedException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            return Result.validationError(e.getMessage());
        } catch (Exception e) {
            log.error("获取上传进度失败: uploadId={}", uploadId, e);
            return Result.error("获取上传进度失败");
        }
    }

    /**
     * 取消上传
     * DELETE /api/upload/{uploadId}
     */
    @DeleteMapping("/{uploadId}")
    @PreAuthorize("hasAuthority('DOCUMENT_EDIT')")
    public Result<Void> cancelUpload(@PathVariable String uploadId) {
        try {
            chunkedUploadService.cancelUpload(
                    uploadId, securityUtils.getCurrentUserId(), securityUtils.isAdmin());
            return Result.success();
        } catch (AccessDeniedException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            return Result.validationError(e.getMessage());
        } catch (Exception e) {
            log.error("取消上传失败: uploadId={}", uploadId, e);
            return Result.error("取消上传失败");
        }
    }
}
