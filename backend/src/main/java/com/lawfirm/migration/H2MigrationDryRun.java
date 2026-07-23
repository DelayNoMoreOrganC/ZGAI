package com.lawfirm.migration;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Copies the offline H2 source into a fresh in-memory schema generated from the
 * current entities. This validates source/schema compatibility without writing
 * to PostgreSQL; it does not replace a real PostgreSQL migration drill.
 */
public final class H2MigrationDryRun {

    private H2MigrationDryRun() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> environment = System.getenv();
        String sourceUrl = required(environment, "MIGRATION_H2_URL");
        String sourceSha256 = required(environment, "MIGRATION_SOURCE_SHA256").toLowerCase(Locale.ROOT);
        String reportValue = required(environment, "MIGRATION_REPORT_PATH");
        if (!sourceUrl.startsWith("jdbc:h2:file:") || !"true".equals(environment.get("MIGRATION_SOURCE_COPY"))) {
            throw new IllegalArgumentException("演练只能读取停机生成的 H2 文件副本");
        }
        if (!sourceSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("MIGRATION_SOURCE_SHA256 必须是 64 位十六进制摘要");
        }
        MigrationSourceValidator.validateOfflineCopy(sourceUrl, sourceSha256);

        Path reportPath = Paths.get(reportValue).toAbsolutePath().normalize();
        ensureReportDirectory(reportPath);
        String targetUrl = "jdbc:h2:mem:zgai_migration_dry_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

        MigrationReport report = new MigrationReport();
        report.setMigrationId(UUID.randomUUID().toString());
        report.setAction("DRY_RUN");
        report.setStatus("RUNNING");
        report.setTargetDatabase("H2_SCHEMA_COMPATIBILITY");
        report.setSourceSha256(sourceSha256);
        report.setStartedAt(OffsetDateTime.now().toString());
        ObjectMapper mapper = new ObjectMapper();

        try {
            TargetSchemaGenerator.createH2CompatibilitySchema(targetUrl);
            try (Connection source = DriverManager.getConnection(sourceUrl, "sa", "");
                 Connection target = DriverManager.getConnection(targetUrl, "sa", "")) {
                report.setSourceProduct(source.getMetaData().getDatabaseProductName() + " "
                        + source.getMetaData().getDatabaseProductVersion());
                report.setTargetProduct("H2 PostgreSQL compatibility schema "
                        + target.getMetaData().getDatabaseProductVersion());
                new MigrationDatabaseCopier(source, target).migrate(report);
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), report);
            System.out.println("migration-status=" + report.getStatus());
            System.out.println("migration-report=" + reportPath);
        } catch (Exception e) {
            report.setStatus("FAILED");
            report.setCompletedAt(OffsetDateTime.now().toString());
            report.setErrorMessage(safeMessage(e));
            mapper.writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), report);
            System.err.println("migration-status=FAILED");
            System.err.println("migration-report=" + reportPath);
            throw e;
        }
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " 未配置");
        return value.trim();
    }

    private static void ensureReportDirectory(Path reportPath) throws Exception {
        Path parent = reportPath.getParent();
        if (parent == null) throw new IllegalArgumentException("迁移报告路径无效");
        if (!Files.isDirectory(parent)) Files.createDirectories(parent);
    }

    private static String safeMessage(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException) {
                return "数据库操作失败，SQLState=" + ((SQLException) current).getSQLState();
            }
            current = current.getCause();
        }
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) return "迁移兼容演练失败";
        String sanitized = message.replaceAll("(?i)(password|secret|token)=[^&\\s]+", "$1=***");
        return exception.getClass().getSimpleName() + ": "
                + sanitized.substring(0, Math.min(sanitized.length(), 500));
    }
}
