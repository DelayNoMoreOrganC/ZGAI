package com.lawfirm.migration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

final class MigrationSourceValidator {

    private static final String H2_FILE_PREFIX = "jdbc:h2:file:";

    private MigrationSourceValidator() {
    }

    static Path validateOfflineCopy(String sourceUrl, String expectedSha256) throws IOException {
        if (sourceUrl == null || !sourceUrl.startsWith(H2_FILE_PREFIX)) {
            throw new IllegalArgumentException("迁移源必须是 H2 文件数据库");
        }
        if (!hasReadOnlyOption(sourceUrl)) {
            throw new IllegalArgumentException("迁移源必须配置 ACCESS_MODE_DATA=r");
        }

        int optionsStart = sourceUrl.indexOf(';', H2_FILE_PREFIX.length());
        String databasePath = optionsStart < 0
                ? sourceUrl.substring(H2_FILE_PREFIX.length())
                : sourceUrl.substring(H2_FILE_PREFIX.length(), optionsStart);
        Path basePath = Paths.get(databasePath);
        if (!basePath.isAbsolute()) {
            throw new IllegalArgumentException("迁移源必须使用绝对路径");
        }

        Path dataFile = Paths.get(databasePath + ".mv.db").toAbsolutePath().normalize();
        if (!Files.isRegularFile(dataFile, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(dataFile)) {
            throw new IllegalArgumentException("迁移源副本不存在或是符号链接");
        }

        String actualSha256 = sha256(dataFile);
        if (!MessageDigest.isEqual(actualSha256.getBytes(StandardCharsets.US_ASCII),
                expectedSha256.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException("迁移源副本 SHA-256 校验失败");
        }
        return dataFile;
    }

    private static boolean hasReadOnlyOption(String sourceUrl) {
        String[] segments = sourceUrl.split(";");
        for (int i = 1; i < segments.length; i++) {
            if ("ACCESS_MODE_DATA=r".equalsIgnoreCase(segments[i].trim())) return true;
        }
        return false;
    }

    private static String sha256(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", e);
        }
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        StringBuilder result = new StringBuilder(64);
        for (byte value : digest.digest()) result.append(String.format("%02x", value));
        return result.toString();
    }
}
