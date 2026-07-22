package com.lawfirm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Performs read-only checks for storage used by case files, knowledge documents and backups.
 */
@Service
public class StorageHealthService {

    @Value("${file.case-library-root:./case-files}")
    private String caseLibraryRoot;

    @Value("${file.knowledge-library-root:./knowledge-files}")
    private String knowledgeLibraryRoot;

    @Value("${backup.base-dir:./backups}")
    private String backupRoot;

    public Map<String, Object> getStorageStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("caseLibrary", inspect("案件文件库", caseLibraryRoot));
        result.put("knowledgeLibrary", inspect("知识库原件", knowledgeLibraryRoot));
        result.put("backup", inspect("数据库备份", backupRoot));
        return result;
    }

    Map<String, Object> inspect(String label, String configuredPath) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("label", label);
        status.put("configured", configuredPath != null && !configuredPath.trim().isEmpty());
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            status.put("status", "missing");
            return status;
        }

        try {
            Path path = Paths.get(configuredPath).toAbsolutePath().normalize();
            boolean exists = Files.exists(path);
            boolean directory = exists && Files.isDirectory(path);
            boolean readable = directory && Files.isReadable(path);
            boolean writable = directory && Files.isWritable(path);
            status.put("exists", exists);
            status.put("directory", directory);
            status.put("readable", readable);
            status.put("writable", writable);
            if (!directory) {
                status.put("status", "missing");
                return status;
            }

            FileStore fileStore = Files.getFileStore(path);
            long totalBytes = fileStore.getTotalSpace();
            long usableBytes = fileStore.getUsableSpace();
            status.put("totalBytes", totalBytes);
            status.put("usableBytes", usableBytes);
            status.put("storageClass", isNetworkStorage(fileStore.type()) ? "network" : "local");
            status.put("status", readable && writable ? "ready" : readable ? "readonly" : "unavailable");
        } catch (Exception e) {
            status.put("status", "unavailable");
        }
        return status;
    }

    private boolean isNetworkStorage(String type) {
        String normalized = type == null ? "" : type.toLowerCase();
        return normalized.contains("smb") || normalized.contains("nfs") || normalized.contains("cifs");
    }
}
