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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 数据备份服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final DataBackupRepository dataBackupRepository;

    @Value("${backup.base.dir:D:/lawfirm/backups}")
    private String backupBaseDir;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Value("${backup.retention.days:180}")
    private Integer retentionDays;

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
    @Transactional(rollbackFor = Exception.class)
    public DataBackup manualBackup(String remark, Long userId) {
        log.info("开始执行手动数据备份，操作人: {}", userId);
        return performBackup("MANUAL", remark, userId);
    }

    /**
     * 执行备份
     */
    @Transactional(rollbackFor = Exception.class)
    private DataBackup performBackup(String backupType, String remark, Long... userIds) {
        Long userId = userIds.length > 0 ? userIds[0] : 0L;

        // 创建备份记录
        DataBackup backup = new DataBackup();
        backup.setBackupType(backupType);
        backup.setBackupTime(LocalDateTime.now());
        backup.setCreatedBy(userId);
        backup.setRetentionDays(retentionDays);
        backup.setRemark(remark);

        try {
            // 确保备份目录存在
            Path backupPath = Paths.get(backupBaseDir);
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
            }

            // 生成备份文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = "lawfirm_backup_" + timestamp + ".sql";
            String backupFilePath = backupPath.resolve(backupFileName).toString();

            // 执行数据库备份
            boolean success = backupDatabase(backupFilePath);

            if (success) {
                // 获取文件大小
                File backupFile = new File(backupFilePath);
                long fileSize = backupFile.length();

                // 更新备份记录
                backup.setFilePath(backupFilePath);
                backup.setFileSize(fileSize);
                backup.setBackupStatus("SUCCESS");
                dataBackupRepository.save(backup);

                log.info("数据备份成功: 文件={}, 大小={}字节", backupFilePath, fileSize);
            } else {
                throw new RuntimeException("数据库备份执行失败");
            }

        } catch (Exception e) {
            log.error("数据备份失败: {}", e.getMessage(), e);
            backup.setBackupStatus("FAILED");
            backup.setErrorMessage(e.getMessage());
            dataBackupRepository.save(backup);
            throw new RuntimeException("数据备份失败: " + e.getMessage(), e);
        }

        return backup;
    }

    /**
     * 备份数据库（支持H2和MySQL）
     */
    private boolean backupDatabase(String backupFilePath) {
        try {
            // 检测数据库类型
            if (datasourceUrl.contains("h2")) {
                return backupH2Database(backupFilePath);
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
                    File backupFile = new File(backup.getFilePath());
                    if (backupFile.exists()) {
                        boolean deleted = backupFile.delete();
                        if (deleted) {
                            log.info("删除过期备份文件: {}", backup.getFilePath());
                            deletedCount++;
                        } else {
                            log.warn("删除备份文件失败: {}", backup.getFilePath());
                        }
                    }

                    // 标记记录为已删除
                    backup.setDeleted(true);
                    dataBackupRepository.save(backup);

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

        File backupFile = new File(backup.getFilePath());
        if (!backupFile.exists()) {
            throw new RuntimeException("备份文件不存在: " + backup.getFilePath());
        }

        try {
            // 检测数据库类型并执行相应的恢复
            if (datasourceUrl.contains("h2")) {
                return restoreH2Database(backup.getFilePath());
            } else if (datasourceUrl.contains("mysql")) {
                return restoreMySQLDatabase(backup.getFilePath());
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
            File backupFile = new File(backup.getFilePath());
            if (backupFile.exists()) {
                backupFile.delete();
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
}
