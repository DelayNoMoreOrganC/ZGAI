package com.lawfirm.service;

import com.lawfirm.entity.DataBackup;
import com.lawfirm.repository.DataBackupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据备份服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final DataBackupRepository dataBackupRepository;

    @Value("${backup.base-dir:./backups}")
    private String backupBaseDir;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Value("${backup.retention-days:180}")
    private Integer retentionDays;

    @Value("${backup.postgres.pg-dump-path:pg_dump}")
    private String pgDumpPath;

    @Value("${backup.postgres.pg-restore-path:pg_restore}")
    private String pgRestorePath;

    /**
     * 每天凌晨2点执行自动备份
     * cron表达式: 秒 分 时 日 月 周
     * 0 0 2 * * ? = 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoBackup() {
        log.info("开始执行自动数据备份...");
        try {
            performBackup("AUTO", null);
            cleanExpiredBackups();
        } catch (Exception e) {
            log.error("自动备份失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 手动触发备份
     */
    public DataBackup manualBackup(String remark, Long userId) {
        log.info("开始执行手动数据备份，操作人: {}", userId);
        return performBackup("MANUAL", remark, userId);
    }

    /**
     * 执行备份
     */
    private DataBackup performBackup(String backupType, String remark, Long... userIds) {
        Long userId = userIds.length > 0 ? userIds[0] : 0L;

        // 创建备份记录
        DataBackup backup = new DataBackup();
        backup.setBackupType(backupType);
        backup.setBackupTime(LocalDateTime.now());
        backup.setCreatedBy(userId);
        backup.setRetentionDays(retentionDays);
        backup.setRemark(remark);
        backup.setFilePath("");

        Path temporaryFile = null;
        Path finalFile = null;
        try {
            // 确保备份目录存在
            Path backupPath = backupRoot();
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
            }
            if (!Files.isDirectory(backupPath) || !Files.isWritable(backupPath)) {
                throw new IOException("备份目录不可写");
            }

            // 生成备份文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String backupFileName = "lawfirm_backup_" + timestamp + getBackupFileExtension();
            finalFile = backupPath.resolve(backupFileName).normalize();
            temporaryFile = backupPath.resolve(backupFileName + ".part").normalize();
            backup.setFilePath(finalFile.toString());

            // 执行数据库备份
            boolean success = backupDatabase(temporaryFile.toString());

            if (success) {
                if (!verifyBackupFile(temporaryFile)) {
                    throw new IOException("备份文件完整性校验失败");
                }
                moveAtomically(temporaryFile, finalFile);

                // 获取文件大小
                long fileSize = Files.size(finalFile);

                // 更新备份记录
                backup.setFileSize(fileSize);
                backup.setBackupStatus("SUCCESS");
                backup.setContentSha256(sha256(finalFile));
                backup.setVerificationStatus("VERIFIED");
                backup.setVerifiedAt(LocalDateTime.now());
                dataBackupRepository.save(backup);

                log.info("数据备份成功: 文件={}, 大小={}字节", finalFile.getFileName(), fileSize);
            } else {
                throw new RuntimeException("数据库备份执行失败");
            }

        } catch (Exception e) {
            log.error("数据备份失败: {}", e.getMessage(), e);
            deleteQuietly(temporaryFile);
            deleteQuietly(finalFile);
            backup.setBackupStatus("FAILED");
            backup.setVerificationStatus("FAILED");
            backup.setErrorMessage(e.getMessage());
            dataBackupRepository.save(backup);
            throw new RuntimeException("数据备份失败: " + e.getMessage(), e);
        }

        return backup;
    }

    /**
     * 备份数据库（支持 H2、MySQL 和 PostgreSQL）
     */
    private boolean backupDatabase(String backupFilePath) {
        try {
            // 检测数据库类型
            if (datasourceUrl.contains("h2")) {
                return backupH2Database(backupFilePath);
            } else if (datasourceUrl.contains("postgresql")) {
                return backupPostgreSQLDatabase(backupFilePath);
            } else if (datasourceUrl.contains("mysql")) {
                return backupMySQLDatabase(backupFilePath);
            } else {
                log.error("不支持的数据库类型: {}", datasourceUrl);
                return false;
            }
        } catch (Exception e) {
            log.error("执行数据库备份异常: {}", e.getMessage(), e);
            return false;
        }
    }

    private String getBackupFileExtension() {
        return datasourceUrl.contains("postgresql") ? ".dump" : ".sql";
    }

    /**
     * 使用 PostgreSQL custom format 备份，密码只通过进程环境变量传递。
     */
    private boolean backupPostgreSQLDatabase(String backupFilePath) {
        try {
            PostgreSqlConnectionInfo connection = parsePostgreSqlConnection(datasourceUrl);
            List<String> command = new ArrayList<>();
            command.add(pgDumpPath);
            command.add("--host=" + connection.host);
            command.add("--port=" + connection.port);
            command.add("--username=" + datasourceUsername);
            command.add("--format=custom");
            command.add("--no-owner");
            command.add("--no-privileges");
            command.add("--file=" + backupFilePath);
            command.add(connection.database);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            if (datasourcePassword != null && !datasourcePassword.isEmpty()) {
                processBuilder.environment().put("PGPASSWORD", datasourcePassword);
            }

            Process process = processBuilder.start();
            String output = readProcessOutput(process);
            int exitCode = process.waitFor();
            Path backupFile = Paths.get(backupFilePath);
            boolean success = exitCode == 0 && Files.exists(backupFile) && Files.size(backupFile) > 0;
            if (!success) {
                log.error("PostgreSQL数据库备份失败: exitCode={}, output={}", exitCode, output);
            }
            return success;
        } catch (Exception e) {
            log.error("执行PostgreSQL备份异常: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean verifyBackupFile(Path backupFile) {
        try {
            if (backupFile == null || !Files.isRegularFile(backupFile, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(backupFile) || Files.size(backupFile) <= 0) {
                return false;
            }
            if (!datasourceUrl.contains("postgresql")) {
                return true;
            }

            List<String> command = new ArrayList<>();
            command.add(pgRestorePath);
            command.add("--list");
            command.add(backupFile.toString());
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = readProcessOutput(process);
            int exitCode = process.waitFor();
            if (exitCode != 0 || output.trim().isEmpty()) {
                log.error("PostgreSQL备份校验失败: exitCode={}, output={}", exitCode, output);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("备份文件校验异常: {}", e.getMessage());
            return false;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean verifyBackup(Long backupId) {
        DataBackup backup = dataBackupRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("备份记录不存在"));
        if (!"SUCCESS".equals(backup.getBackupStatus())) {
            throw new RuntimeException("只能校验成功生成的备份");
        }

        Path backupFile = resolveManagedBackupPath(backup.getFilePath());
        boolean verified = verifyBackupFile(backupFile);
        backup.setVerificationStatus(verified ? "VERIFIED" : "FAILED");
        backup.setVerifiedAt(LocalDateTime.now());
        if (verified) {
            try {
                backup.setFileSize(Files.size(backupFile));
                backup.setContentSha256(sha256(backupFile));
                backup.setErrorMessage(null);
            } catch (IOException e) {
                throw new RuntimeException("读取备份文件失败", e);
            }
        } else {
            backup.setErrorMessage("备份文件完整性校验失败");
        }
        dataBackupRepository.save(backup);
        return verified;
    }

    static PostgreSqlConnectionInfo parsePostgreSqlConnection(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("无效的PostgreSQL JDBC地址");
        }
        URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
        String path = uri.getPath();
        if (path == null || path.length() <= 1) {
            throw new IllegalArgumentException("PostgreSQL JDBC地址缺少数据库名");
        }
        if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
            throw new IllegalArgumentException("PostgreSQL JDBC地址缺少主机名");
        }
        return new PostgreSqlConnectionInfo(
                uri.getHost(),
                uri.getPort() > 0 ? uri.getPort() : 5432,
                path.substring(1));
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() < 4000) {
                    output.append(line).append('\n');
                }
            }
        }
        return output.toString().trim();
    }

    static class PostgreSqlConnectionInfo {
        final String host;
        final int port;
        final String database;

        PostgreSqlConnectionInfo(String host, int port, String database) {
            this.host = host;
            this.port = port;
            this.database = database;
        }
    }

    /**
     * 使用H2 SCRIPT命令备份H2数据库
     */
    private boolean backupH2Database(String backupFilePath) {
        try {
            log.info("使用H2 SCRIPT命令备份数据库");

            // H2备份使用SCRIPT SQL命令
            // 这里简化实现：通过JDBC执行SCRIPT命令
            java.sql.Connection connection = null;
            java.sql.Statement statement = null;
            java.sql.ResultSet rs = null;

            try {
                // 使用反射获取DataSource连接
                org.springframework.boot.autoconfigure.jdbc.DataSourceProperties dataSourceProperties =
                    new org.springframework.boot.autoconfigure.jdbc.DataSourceProperties();
                dataSourceProperties.setUrl(datasourceUrl);
                dataSourceProperties.setUsername(datasourceUsername);
                dataSourceProperties.setPassword(datasourcePassword);

                javax.sql.DataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().build();
                connection = dataSource.getConnection();

                statement = connection.createStatement();
                // H2的SCRIPT命令可以导出整个数据库
                String sql = "SCRIPT TO '" + backupFilePath.replace("\\", "/") + "'";
                log.info("执行H2备份命令: {}", sql);

                statement.execute(sql);
                log.info("H2数据库备份成功: {}", backupFilePath);
                return true;

            } finally {
                if (rs != null) rs.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            }

        } catch (Exception e) {
            log.error("H2数据库备份失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 使用mysqldump备份MySQL数据库
     */
    private boolean backupMySQLDatabase(String backupFilePath) {
        try {
            // 从jdbc.url中提取数据库名
            String databaseName = extractDatabaseName(datasourceUrl);

            // 构建mysqldump命令
            // mysqldump -u username -ppassword database > backup.sql
            String command = String.format("mysqldump -u%s -p%s %s -r %s",
                    datasourceUsername,
                    datasourcePassword,
                    databaseName,
                    backupFilePath);

            log.info("执行MySQL备份命令: {}", command.replaceAll("-p[^\\s]+", "-p***"));

            Process process = Runtime.getRuntime().exec(new String[]{"cmd", "/c", command});

            // 等待命令执行完成
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("MySQL数据库备份成功: {}", backupFilePath);
                return true;
            } else {
                // 读取错误信息
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    StringBuilder error = new StringBuilder();
                    while ((line = errorReader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                    log.error("MySQL数据库备份失败, exitCode={}, error={}", exitCode, error.toString());
                } catch (IOException e) {
                    log.error("读取错误信息失败", e);
                }
                return false;
            }

        } catch (Exception e) {
            log.error("执行MySQL数据库备份异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 从jdbc.url中提取数据库名
     * jdbc:mysql://localhost:3306/lawfirm?useSSL=false&serverTimezone=Asia/Shanghai
     */
    private String extractDatabaseName(String jdbcUrl) {
        try {
            // 移除jdbc:mysql://前缀
            String url = jdbcUrl.replace("jdbc:mysql://", "");

            // 找到第一个/的位置（数据库名开始位置）
            int slashIndex = url.indexOf('/');
            if (slashIndex == -1) {
                throw new RuntimeException("无法从JDBC URL中提取数据库名: " + jdbcUrl);
            }

            // 提取数据库名（从/到?之间）
            String dbName = url.substring(slashIndex + 1);
            int questionIndex = dbName.indexOf('?');
            if (questionIndex != -1) {
                dbName = dbName.substring(0, questionIndex);
            }

            log.info("提取数据库名: {}", dbName);
            return dbName;

        } catch (Exception e) {
            log.error("提取数据库名失败: {}", e.getMessage(), e);
            throw new RuntimeException("提取数据库名失败", e);
        }
    }

    /**
     * 清理过期的备份文件（超过保留天数）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanExpiredBackups() {
        log.info("开始清理过期备份文件...");

        try {
            // 计算180天前的日期
            LocalDateTime beforeDate = LocalDateTime.now().minusDays(retentionDays);

            // 查询过期的备份记录
            List<DataBackup> expiredBackups = dataBackupRepository.findBackupsBeforeDate(beforeDate);

            if (expiredBackups.isEmpty()) {
                log.info("没有过期备份需要清理");
                return;
            }

            int deletedCount = 0;
            for (DataBackup backup : expiredBackups) {
                try {
                    // 删除物理文件
                    Path backupFile = resolveManagedBackupPath(backup.getFilePath());
                    boolean deleted = !Files.exists(backupFile) || Files.deleteIfExists(backupFile);
                    if (!deleted) {
                        log.warn("删除备份文件失败，保留备份记录: backupId={}", backup.getId());
                        continue;
                    }

                    // 标记记录为已删除
                    backup.setDeleted(true);
                    dataBackupRepository.save(backup);
                    deletedCount++;

                } catch (Exception e) {
                    log.error("删除过期备份失败: {}", e.getMessage(), e);
                }
            }

            log.info("过期备份清理完成，共删除 {} 个备份文件", deletedCount);

        } catch (Exception e) {
            log.error("清理过期备份失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取备份列表
     */
    public List<DataBackup> getBackupList() {
        return dataBackupRepository.findRecentBackups();
    }

    /**
     * 恢复数据（危险操作，需谨慎）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreData(Long backupId, Long userId) {
        log.warn("用户 {} 尝试从备份 {} 恢复数据", userId, backupId);

        DataBackup backup = dataBackupRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("备份记录不存在"));

        if (!"SUCCESS".equals(backup.getBackupStatus())) {
            throw new RuntimeException("只能从成功的备份恢复数据");
        }

        Path backupFile = resolveManagedBackupPath(backup.getFilePath());
        if (!Files.exists(backupFile)) {
            throw new RuntimeException("备份文件不存在");
        }

        try {
            // 检测数据库类型并执行相应的恢复
            if (datasourceUrl.contains("h2")) {
                return restoreH2Database(backupFile.toString());
            } else if (datasourceUrl.contains("postgresql")) {
                throw new RuntimeException("PostgreSQL恢复必须停机后由管理员使用pg_restore执行，系统内不允许在线覆盖主库");
            } else if (datasourceUrl.contains("mysql")) {
                return restoreMySQLDatabase(backupFile.toString());
            } else {
                throw new RuntimeException("不支持的数据库类型: " + datasourceUrl);
            }

        } catch (Exception e) {
            log.error("恢复数据异常: {}", e.getMessage(), e);
            throw new RuntimeException("恢复数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 恢复H2数据库
     */
    private boolean restoreH2Database(String backupFilePath) {
        try {
            log.info("使用H2恢复数据库: {}", backupFilePath);

            java.sql.Connection connection = null;
            java.sql.Statement statement = null;
            java.io.BufferedReader reader = null;

            try {
                // 使用反射获取DataSource连接
                org.springframework.boot.autoconfigure.jdbc.DataSourceProperties dataSourceProperties =
                    new org.springframework.boot.autoconfigure.jdbc.DataSourceProperties();
                dataSourceProperties.setUrl(datasourceUrl);
                dataSourceProperties.setUsername(datasourceUsername);
                dataSourceProperties.setPassword(datasourcePassword);

                javax.sql.DataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().build();
                connection = dataSource.getConnection();
                statement = connection.createStatement();

                // 读取备份文件并逐行执行
                reader = new java.io.BufferedReader(new java.io.FileReader(backupFilePath));
                String line;
                int lineCount = 0;

                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty() && !line.trim().startsWith("--")) {
                        statement.execute(line);
                        lineCount++;

                        // 每1000行提交一次
                        if (lineCount % 1000 == 0) {
                            connection.commit();
                            log.debug("已恢复 {} 行", lineCount);
                        }
                    }
                }

                connection.commit();
                log.info("H2数据库恢复成功，共执行 {} 行", lineCount);
                return true;

            } finally {
                if (reader != null) reader.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            }

        } catch (Exception e) {
            log.error("H2数据库恢复失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 恢复MySQL数据库
     */
    private boolean restoreMySQLDatabase(String backupFilePath) {
        try {
            // 从jdbc.url中提取数据库名
            String databaseName = extractDatabaseName(datasourceUrl);

            // 构建mysql恢复命令
            // mysql -u username -ppassword database < backup.sql
            String command = String.format("mysql -u%s -p%s %s < %s",
                    datasourceUsername,
                    datasourcePassword,
                    databaseName,
                    backupFilePath);

            log.info("执行MySQL恢复命令: {}", command.replaceAll("-p[^\\s]+", "-p***"));

            Process process = Runtime.getRuntime().exec(new String[]{"cmd", "/c", command});

            // 等待命令执行完成
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("MySQL数据库恢复成功: {}", backupFilePath);
                return true;
            } else {
                // 读取错误信息
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    StringBuilder error = new StringBuilder();
                    while ((line = errorReader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                    log.error("MySQL数据库恢复失败, exitCode={}, error={}", exitCode, error.toString());
                    throw new RuntimeException("数据库恢复失败: " + error.toString());
                } catch (IOException e) {
                    log.error("读取错误信息失败", e);
                    throw new RuntimeException("读取错误信息失败", e);
                }
            }

        } catch (Exception e) {
            log.error("执行MySQL数据库恢复异常: {}", e.getMessage(), e);
            throw new RuntimeException("恢复数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除备份
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBackup(Long backupId) {
        try {
            DataBackup backup = dataBackupRepository.findById(backupId)
                    .orElseThrow(() -> new RuntimeException("备份记录不存在"));

            // 删除物理文件
            Path backupFile = resolveManagedBackupPath(backup.getFilePath());
            if (Files.exists(backupFile)) {
                try {
                    Files.delete(backupFile);
                } catch (IOException e) {
                    log.warn("备份文件删除失败，保留数据库记录: backupId={}", backupId);
                    return false;
                }
            }

            // 标记记录为已删除
            backup.setDeleted(true);
            dataBackupRepository.save(backup);

            log.info("删除备份成功: backupId={}", backupId);
            return true;

        } catch (Exception e) {
            log.error("删除备份失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private Path backupRoot() {
        return Paths.get(backupBaseDir).toAbsolutePath().normalize();
    }

    private Path resolveManagedBackupPath(String storedPath) {
        if (storedPath == null || storedPath.trim().isEmpty()) {
            throw new SecurityException("备份文件路径无效");
        }
        try {
            Path root = backupRoot();
            Path candidate = Paths.get(storedPath).toAbsolutePath().normalize();
            if (!candidate.startsWith(root) || Files.isSymbolicLink(candidate)) {
                throw new SecurityException("备份文件路径超出受控目录");
            }
            if (Files.exists(root) && Files.exists(candidate)) {
                Path realRoot = root.toRealPath();
                Path realCandidate = candidate.toRealPath();
                if (!realCandidate.startsWith(realRoot)) {
                    throw new SecurityException("备份文件真实路径超出受控目录");
                }
            }
            return candidate;
        } catch (IOException e) {
            throw new RuntimeException("无法校验备份文件路径", e);
        }
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException cleanupError) {
            log.warn("清理备份半成品失败: {}", path.getFileName());
        }
    }

    static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) {
                        digest.update(buffer, 0, read);
                    }
                }
            }
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest.digest()) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前运行环境不支持SHA-256", e);
        }
    }
}
