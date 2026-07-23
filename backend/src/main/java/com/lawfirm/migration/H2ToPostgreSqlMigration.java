package com.lawfirm.migration;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Offline migration entry point. It reads an immutable H2 copy and writes only
 * to a separately confirmed, empty PostgreSQL database.
 */
public final class H2ToPostgreSqlMigration {

    private H2ToPostgreSqlMigration() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> environment = System.getenv();
        String action = required(environment, "MIGRATION_ACTION").toUpperCase(Locale.ROOT);
        if (!"PLAN".equals(action) && !"EXECUTE".equals(action)) {
            throw new IllegalArgumentException("MIGRATION_ACTION 只能是 PLAN 或 EXECUTE");
        }

        String sourceUrl = required(environment, "MIGRATION_H2_URL");
        String targetUrl = required(environment, "POSTGRES_URL");
        String targetUsername = required(environment, "POSTGRES_USER");
        String targetPassword = required(environment, "POSTGRES_PASSWORD");
        String sourceSha256 = required(environment, "MIGRATION_SOURCE_SHA256").toLowerCase(Locale.ROOT);
        String reportPathValue = required(environment, "MIGRATION_REPORT_PATH");
        String targetDatabase = postgreSqlDatabaseName(targetUrl);

        if (!sourceUrl.startsWith("jdbc:h2:file:") || !"true".equals(environment.get("MIGRATION_SOURCE_COPY"))) {
            throw new IllegalArgumentException("迁移只能读取停机生成的 H2 文件副本");
        }
        if (!sourceSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("MIGRATION_SOURCE_SHA256 必须是 64 位十六进制摘要");
        }
        MigrationSourceValidator.validateOfflineCopy(sourceUrl, sourceSha256);
        if ("EXECUTE".equals(action)
                && !targetDatabase.equals(environment.get("MIGRATION_CONFIRM_DATABASE"))) {
            throw new IllegalArgumentException("执行迁移前必须用 MIGRATION_CONFIRM_DATABASE 二次确认目标库名");
        }

        Path reportPath = Paths.get(reportPathValue).toAbsolutePath().normalize();
        if (reportPath.getParent() == null) throw new IllegalArgumentException("迁移报告路径无效");
        if (!Files.isDirectory(reportPath.getParent())) Files.createDirectories(reportPath.getParent());

        MigrationReport report = new MigrationReport();
        report.setMigrationId(UUID.randomUUID().toString());
        report.setAction(action);
        report.setStatus("RUNNING");
        report.setTargetDatabase(targetDatabase);
        report.setSourceSha256(sourceSha256);
        report.setStartedAt(OffsetDateTime.now().toString());

        ObjectMapper mapper = new ObjectMapper();
        try {
            Class.forName("org.h2.Driver");
            Class.forName("org.postgresql.Driver");
            try (Connection source = DriverManager.getConnection(sourceUrl, "sa", "");
                 Connection target = DriverManager.getConnection(targetUrl, targetUsername, targetPassword)) {
                validateProducts(source, target);
                report.setSourceProduct(source.getMetaData().getDatabaseProductName() + " "
                        + source.getMetaData().getDatabaseProductVersion());
                report.setTargetProduct(target.getMetaData().getDatabaseProductName() + " "
                        + target.getMetaData().getDatabaseProductVersion());

                MigrationDatabaseCopier copier = new MigrationDatabaseCopier(source, target);
                if ("PLAN".equals(action)) {
                    copier.plan(report);
                    report.setCompletedAt(OffsetDateTime.now().toString());
                } else {
                    if (MigrationDatabaseCopier.hasAnyRows(target, target.getSchema())) {
                        throw new IllegalStateException("目标 PostgreSQL 已有业务数据，迁移已停止");
                    }
                }
            }

            if ("EXECUTE".equals(action)) {
                TargetSchemaGenerator.updatePostgreSqlSchema(targetUrl, targetUsername, targetPassword);
                try (Connection source = DriverManager.getConnection(sourceUrl, "sa", "");
                     Connection target = DriverManager.getConnection(targetUrl, targetUsername, targetPassword)) {
                    new MigrationDatabaseCopier(source, target).migrate(report);
                }
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), report);
            System.out.println("migration-status=" + report.getStatus());
            System.out.println("migration-report=" + reportPath);
            if ("BLOCKED".equals(report.getStatus())) {
                throw new IllegalStateException("目标库已有数据，迁移计划被阻断");
            }
        } catch (Exception e) {
            if (!"BLOCKED".equals(report.getStatus())) report.setStatus("FAILED");
            report.setCompletedAt(OffsetDateTime.now().toString());
            report.setErrorMessage(safeMessage(e));
            mapper.writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), report);
            System.err.println("migration-status=FAILED");
            System.err.println("migration-report=" + reportPath);
            throw e;
        }
    }

    static String postgreSqlDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("POSTGRES_URL 必须是 PostgreSQL JDBC 地址");
        }
        URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
        if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
            throw new IllegalArgumentException("POSTGRES_URL 缺少主机名");
        }
        String path = uri.getPath();
        if (path == null || !path.matches("/[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("POSTGRES_URL 必须包含简单且明确的数据库名");
        }
        String database = path.substring(1);
        if ("postgres".equalsIgnoreCase(database) || "template0".equalsIgnoreCase(database)
                || "template1".equalsIgnoreCase(database)) {
            throw new IllegalArgumentException("禁止把系统数据库作为迁移目标");
        }
        return database;
    }

    private static void validateProducts(Connection source, Connection target) throws Exception {
        String sourceProduct = source.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        String targetProduct = target.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        if (!sourceProduct.contains("h2")) throw new IllegalArgumentException("迁移源不是 H2 数据库");
        if (!targetProduct.contains("postgresql")) throw new IllegalArgumentException("迁移目标不是 PostgreSQL");
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " 未配置");
        return value.trim();
    }

    private static String safeMessage(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof java.sql.SQLException) {
                return "数据库操作失败，SQLState=" + ((java.sql.SQLException) current).getSQLState();
            }
            current = current.getCause();
        }
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) return exception.getClass().getSimpleName();
        String sanitized = message.replaceAll("(?i)(password|secret|token)=[^&\\s]+", "$1=***");
        return exception.getClass().getSimpleName() + ": "
                + sanitized.substring(0, Math.min(sanitized.length(), 500));
    }
}
