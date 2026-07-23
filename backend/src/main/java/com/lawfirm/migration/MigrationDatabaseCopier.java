package com.lawfirm.migration;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

final class MigrationDatabaseCopier {

    private static final String AUDIT_TABLE = "zgai_data_migration_audit";
    private static final int BATCH_SIZE = 200;

    private final Connection source;
    private final Connection target;
    private final String sourceSchema;
    private final String targetSchema;

    MigrationDatabaseCopier(Connection source, Connection target) throws SQLException {
        this.source = source;
        this.target = target;
        this.sourceSchema = schemaOrDefault(source, "PUBLIC");
        this.targetSchema = schemaOrDefault(target, "public");
    }

    MigrationReport plan(MigrationReport report) throws SQLException {
        Map<String, String> sourceTables = listTables(source, sourceSchema, report.getWarnings());
        Map<String, String> targetTables = listTables(target, targetSchema, report.getWarnings());
        boolean blocked = false;

        for (Map.Entry<String, String> entry : sourceTables.entrySet()) {
            MigrationReport.TableResult result = new MigrationReport.TableResult();
            result.setSourceTable(entry.getValue());
            result.setSourceRows(countRows(source, sourceSchema, entry.getValue()));
            report.setSourceRowTotal(report.getSourceRowTotal() + result.getSourceRows());

            String targetTable = targetTables.get(entry.getKey());
            result.setTargetTable(targetTable);
            if (targetTable == null) {
                result.setStatus("SCHEMA_PENDING");
            } else {
                long targetRows = countRows(target, targetSchema, targetTable);
                result.setTargetRows(targetRows);
                report.setTargetRowTotal(report.getTargetRowTotal() + targetRows);
                result.setStatus(targetRows == 0 ? "READY" : "TARGET_NOT_EMPTY");
                blocked |= targetRows > 0;
            }
            report.getTables().add(result);
        }
        for (Map.Entry<String, String> entry : targetTables.entrySet()) {
            if (sourceTables.containsKey(entry.getKey())) continue;
            long rows = countRows(target, targetSchema, entry.getValue());
            if (rows > 0) {
                blocked = true;
                report.getWarnings().add("目标库额外非空表: " + entry.getValue() + "=" + rows);
            }
        }
        report.setStatus(blocked ? "BLOCKED" : "READY");
        return report;
    }

    MigrationReport migrate(MigrationReport report) throws Exception {
        Map<String, String> sourceTables = listTables(source, sourceSchema, report.getWarnings());
        Map<String, String> targetTables = listTables(target, targetSchema, report.getWarnings());
        ensureEverySourceTableHasTarget(sourceTables, targetTables);
        ensureTargetBusinessTablesEmpty(targetTables.values());

        List<String> orderedTargetTables = dependencyOrder(target, targetSchema,
                targetNamesForSource(sourceTables, targetTables));
        Map<String, String> sourceByTarget = new HashMap<>();
        for (Map.Entry<String, String> sourceTable : sourceTables.entrySet()) {
            sourceByTarget.put(normalize(targetTables.get(sourceTable.getKey())), sourceTable.getValue());
        }

        boolean originalAutoCommit = target.getAutoCommit();
        target.setAutoCommit(false);
        try {
            for (String targetTable : orderedTargetTables) {
                String sourceTable = sourceByTarget.get(normalize(targetTable));
                MigrationReport.TableResult tableResult = copyTable(sourceTable, targetTable);
                report.getTables().add(tableResult);
                if (tableResult.getPrimaryKeyColumns().isEmpty()) {
                    report.getWarnings().add("表 " + sourceTable + " 没有主键，仅完成行数校验");
                }
                report.setSourceRowTotal(report.getSourceRowTotal() + tableResult.getSourceRows());
                report.setTargetRowTotal(report.getTargetRowTotal() + tableResult.getTargetRows());
            }

            if (isPostgreSql(target)) {
                resetPostgreSqlSequences(orderedTargetTables);
            }
            report.setStatus("SUCCESS");
            report.setCompletedAt(OffsetDateTime.now().toString());
            writeAuditRecord(report);
            target.commit();
            return report;
        } catch (Exception e) {
            target.rollback();
            throw e;
        } finally {
            target.setAutoCommit(originalAutoCommit);
        }
    }

    private MigrationReport.TableResult copyTable(String sourceTable, String targetTable) throws Exception {
        List<ColumnInfo> sourceColumns = listColumns(source, sourceSchema, sourceTable);
        List<ColumnInfo> targetColumns = listColumns(target, targetSchema, targetTable);
        Map<String, ColumnInfo> targetByName = byNormalizedName(targetColumns);

        for (ColumnInfo sourceColumn : sourceColumns) {
            if (!targetByName.containsKey(normalize(sourceColumn.name))) {
                throw new IllegalStateException("目标表缺少源字段: " + sourceTable + "." + sourceColumn.name);
            }
        }
        Set<String> sourceNames = new HashSet<>();
        for (ColumnInfo column : sourceColumns) {
            sourceNames.add(normalize(column.name));
        }
        for (ColumnInfo targetColumn : targetColumns) {
            if (!sourceNames.contains(normalize(targetColumn.name)) && !targetColumn.nullable
                    && targetColumn.defaultValue == null && !targetColumn.autoIncrement) {
                throw new IllegalStateException("源表缺少目标必填字段: " + targetTable + "." + targetColumn.name);
            }
        }

        List<String> sourcePrimaryKeys = primaryKeys(source, sourceSchema, sourceTable);
        List<String> targetPrimaryKeys = primaryKeys(target, targetSchema, targetTable);
        if (!normalized(sourcePrimaryKeys).equals(normalized(targetPrimaryKeys))) {
            throw new IllegalStateException("主键结构不一致: " + sourceTable + " -> " + targetTable);
        }

        List<String> sourceColumnNames = columnNames(sourceColumns);
        List<String> targetColumnNames = new ArrayList<>();
        for (ColumnInfo sourceColumn : sourceColumns) {
            targetColumnNames.add(targetByName.get(normalize(sourceColumn.name)).name);
        }
        String sourceColumnList = joinQuoted(sourceColumnNames);
        String targetColumnList = joinQuoted(targetColumnNames);
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < sourceColumns.size(); i++) {
            if (i > 0) placeholders.append(',');
            placeholders.append('?');
        }
        String selectSql = "SELECT " + sourceColumnList + " FROM " + qualified(sourceSchema, sourceTable)
                + orderBy(sourcePrimaryKeys);
        String insertSql = "INSERT INTO " + qualified(targetSchema, targetTable) + " (" + targetColumnList
                + ") VALUES (" + placeholders + ")";

        long copied = 0;
        try (Statement select = source.createStatement();
             ResultSet rows = select.executeQuery(selectSql);
             PreparedStatement insert = target.prepareStatement(insertSql)) {
            while (rows.next()) {
                for (int i = 0; i < sourceColumns.size(); i++) {
                    setValue(insert, i + 1, rows.getObject(i + 1));
                }
                insert.addBatch();
                copied++;
                if (copied % BATCH_SIZE == 0) {
                    insert.executeBatch();
                }
            }
            if (copied % BATCH_SIZE != 0) {
                insert.executeBatch();
            }
        }

        long sourceRows = countRows(source, sourceSchema, sourceTable);
        long targetRows = countRows(target, targetSchema, targetTable);
        if (copied != sourceRows || targetRows != sourceRows) {
            throw new IllegalStateException("迁移行数不一致: " + sourceTable + " source=" + sourceRows
                    + ", copied=" + copied + ", target=" + targetRows);
        }

        String sourcePkHash = primaryKeyDigest(source, sourceSchema, sourceTable, sourcePrimaryKeys);
        String targetPkHash = primaryKeyDigest(target, targetSchema, targetTable, targetPrimaryKeys);
        if (sourcePkHash != null && !sourcePkHash.equals(targetPkHash)) {
            throw new IllegalStateException("迁移主键摘要不一致: " + sourceTable);
        }

        MigrationReport.TableResult result = new MigrationReport.TableResult();
        result.setSourceTable(sourceTable);
        result.setTargetTable(targetTable);
        result.setSourceRows(sourceRows);
        result.setTargetRows(targetRows);
        result.setPrimaryKeyColumns(sourcePrimaryKeys);
        result.setSourcePrimaryKeySha256(sourcePkHash);
        result.setTargetPrimaryKeySha256(targetPkHash);
        result.setStatus("VERIFIED");
        return result;
    }

    private void writeAuditRecord(MigrationReport report) throws Exception {
        try (Statement statement = target.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + qualified(targetSchema, AUDIT_TABLE) + " ("
                    + "migration_id VARCHAR(36) PRIMARY KEY, source_sha256 VARCHAR(64) NOT NULL, "
                    + "started_at TIMESTAMP WITH TIME ZONE NOT NULL, "
                    + "completed_at TIMESTAMP WITH TIME ZONE NOT NULL, "
                    + "source_rows BIGINT NOT NULL, target_rows BIGINT NOT NULL, details_json TEXT NOT NULL)");
        }
        String sql = "INSERT INTO " + qualified(targetSchema, AUDIT_TABLE)
                + " (migration_id, source_sha256, started_at, completed_at, source_rows, target_rows, details_json)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)";
        String details = new ObjectMapper().writeValueAsString(report.getTables());
        try (PreparedStatement statement = target.prepareStatement(sql)) {
            statement.setString(1, report.getMigrationId());
            statement.setString(2, report.getSourceSha256());
            statement.setObject(3, OffsetDateTime.parse(report.getStartedAt()));
            statement.setObject(4, OffsetDateTime.parse(report.getCompletedAt()));
            statement.setLong(5, report.getSourceRowTotal());
            statement.setLong(6, report.getTargetRowTotal());
            statement.setString(7, details);
            statement.executeUpdate();
        }
    }

    private void resetPostgreSqlSequences(Collection<String> migratedTables) throws SQLException {
        Set<String> normalizedTables = normalized(migratedTables);
        String sql = "SELECT table_name, column_name, pg_get_serial_sequence(quote_ident(table_schema) || '.'"
                + " || quote_ident(table_name), column_name) AS sequence_name "
                + "FROM information_schema.columns WHERE table_schema = ? "
                + "AND (is_identity = 'YES' OR column_default LIKE 'nextval(%')";
        try (PreparedStatement statement = target.prepareStatement(sql)) {
            statement.setString(1, targetSchema);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    String table = rows.getString("table_name");
                    String column = rows.getString("column_name");
                    String sequence = rows.getString("sequence_name");
                    if (sequence == null || !normalizedTables.contains(normalize(table))) continue;
                    String reset = "SELECT setval(?::regclass, COALESCE(MAX(" + quoted(column)
                            + "), 1), MAX(" + quoted(column) + ") IS NOT NULL) FROM "
                            + qualified(targetSchema, table);
                    try (PreparedStatement sequenceStatement = target.prepareStatement(reset)) {
                        sequenceStatement.setString(1, sequence);
                        sequenceStatement.executeQuery().close();
                    }
                }
            }
        }
    }

    private static void setValue(PreparedStatement statement, int index, Object value) throws Exception {
        if (value instanceof Clob) {
            statement.setString(index, readClob((Clob) value));
        } else if (value instanceof Blob) {
            statement.setBytes(index, readBlob((Blob) value));
        } else if (value instanceof UUID) {
            statement.setString(index, value.toString());
        } else {
            statement.setObject(index, value);
        }
    }

    private static String readClob(Clob value) throws SQLException, IOException {
        try (Reader reader = value.getCharacterStream()) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                if (read > 0) result.append(buffer, 0, read);
            }
            return result.toString();
        }
    }

    private static byte[] readBlob(Blob value) throws SQLException, IOException {
        try (InputStream input = value.getBinaryStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static String primaryKeyDigest(Connection connection, String schema, String table,
                                           List<String> primaryKeys) throws Exception {
        if (primaryKeys.isEmpty()) return null;
        MessageDigest digest = sha256Digest();
        String sql = "SELECT " + joinQuoted(primaryKeys) + " FROM " + qualified(schema, table)
                + orderBy(primaryKeys);
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                for (int i = 1; i <= primaryKeys.size(); i++) {
                    String value = rows.getString(i);
                    if (value == null) {
                        digest.update(ByteBuffer.allocate(4).putInt(-1).array());
                    } else {
                        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                        digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
                        digest.update(bytes);
                    }
                }
            }
        }
        return hex(digest.digest());
    }

    static List<String> dependencyOrder(Map<String, Set<String>> dependencies) {
        Map<String, Set<String>> remaining = new TreeMap<>();
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            Set<String> relevant = new HashSet<>();
            for (String dependency : entry.getValue()) {
                if (dependencies.containsKey(dependency) && !dependency.equals(entry.getKey())) {
                    relevant.add(dependency);
                }
            }
            remaining.put(entry.getKey(), relevant);
        }
        List<String> ordered = new ArrayList<>();
        while (!remaining.isEmpty()) {
            List<String> ready = new ArrayList<>();
            for (Map.Entry<String, Set<String>> entry : remaining.entrySet()) {
                if (Collections.disjoint(entry.getValue(), remaining.keySet())) ready.add(entry.getKey());
            }
            if (ready.isEmpty()) {
                throw new IllegalStateException("检测到循环外键，无法确定安全迁移顺序: " + remaining.keySet());
            }
            Collections.sort(ready);
            ordered.addAll(ready);
            for (String table : ready) remaining.remove(table);
        }
        return ordered;
    }

    private static List<String> dependencyOrder(Connection connection, String schema,
                                                Collection<String> tables) throws SQLException {
        Map<String, String> actualByNormalized = new HashMap<>();
        for (String table : tables) actualByNormalized.put(normalize(table), table);
        Map<String, Set<String>> dependencies = new LinkedHashMap<>();
        for (String table : tables) {
            Set<String> tableDependencies = new LinkedHashSet<>();
            try (ResultSet keys = connection.getMetaData().getImportedKeys(null, schema, table)) {
                while (keys.next()) {
                    String referenced = actualByNormalized.get(normalize(keys.getString("PKTABLE_NAME")));
                    if (referenced != null) tableDependencies.add(referenced);
                }
            }
            dependencies.put(table, tableDependencies);
        }
        return dependencyOrder(dependencies);
    }

    private void ensureEverySourceTableHasTarget(Map<String, String> sourceTables,
                                                 Map<String, String> targetTables) {
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> entry : sourceTables.entrySet()) {
            if (!targetTables.containsKey(entry.getKey())) missing.add(entry.getValue());
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("目标结构缺少源数据表，迁移已停止: " + missing);
        }
    }

    private void ensureTargetBusinessTablesEmpty(Collection<String> targetTables) throws SQLException {
        List<String> nonEmpty = new ArrayList<>();
        for (String table : targetTables) {
            if (AUDIT_TABLE.equalsIgnoreCase(table)) continue;
            long rows = countRows(target, targetSchema, table);
            if (rows > 0) nonEmpty.add(table + "=" + rows);
        }
        if (!nonEmpty.isEmpty()) {
            throw new IllegalStateException("目标库已有业务数据，拒绝覆盖: " + nonEmpty);
        }
    }

    static boolean hasAnyRows(Connection connection, String schema) throws SQLException {
        for (String table : listTables(connection, schema).values()) {
            if (!AUDIT_TABLE.equalsIgnoreCase(table) && countRows(connection, schema, table) > 0) return true;
        }
        return false;
    }

    private static Map<String, String> listTables(Connection connection, String schema) throws SQLException {
        return listTables(connection, schema, null);
    }

    private static Map<String, String> listTables(Connection connection, String schema,
                                                  List<String> warnings) throws SQLException {
        Map<String, List<String>> candidates = new TreeMap<>();
        try (ResultSet rows = connection.getMetaData().getTables(null, schema, "%", new String[]{"TABLE"})) {
            while (rows.next()) {
                String table = rows.getString("TABLE_NAME");
                if (table == null || AUDIT_TABLE.equalsIgnoreCase(table)) continue;
                candidates.computeIfAbsent(normalize(table), ignored -> new ArrayList<>()).add(table);
            }
        }
        Map<String, String> tables = new TreeMap<>();
        for (Map.Entry<String, List<String>> entry : candidates.entrySet()) {
            List<String> names = entry.getValue();
            if (names.size() == 1) {
                tables.put(entry.getKey(), names.get(0));
                continue;
            }
            List<String> nonEmpty = new ArrayList<>();
            for (String name : names) {
                if (countRows(connection, schema, name) > 0) nonEmpty.add(name);
            }
            if (nonEmpty.size() > 1) {
                throw new IllegalStateException("存在大小写同名且均含数据的表，禁止自动合并: " + names);
            }
            String selected;
            if (nonEmpty.size() == 1) {
                selected = nonEmpty.get(0);
            } else {
                selected = names.stream().filter(name -> name.equals(entry.getKey()))
                        .findFirst().orElseGet(() -> names.stream().sorted().findFirst().get());
            }
            tables.put(entry.getKey(), selected);
            if (warnings != null) {
                List<String> ignored = new ArrayList<>(names);
                ignored.remove(selected);
                warnings.add("忽略空的大小写重复表 " + ignored + "，迁移 " + selected);
            }
        }
        return tables;
    }

    private static List<ColumnInfo> listColumns(Connection connection, String schema, String table)
            throws SQLException {
        Map<Integer, ColumnInfo> ordered = new TreeMap<>();
        try (ResultSet rows = connection.getMetaData().getColumns(null, schema, table, "%")) {
            while (rows.next()) {
                ColumnInfo info = new ColumnInfo();
                info.name = rows.getString("COLUMN_NAME");
                info.nullable = rows.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
                info.defaultValue = rows.getString("COLUMN_DEF");
                info.autoIncrement = "YES".equalsIgnoreCase(rows.getString("IS_AUTOINCREMENT"));
                ordered.put(rows.getInt("ORDINAL_POSITION"), info);
            }
        }
        return new ArrayList<>(ordered.values());
    }

    private static List<String> primaryKeys(Connection connection, String schema, String table)
            throws SQLException {
        Map<Short, String> ordered = new TreeMap<>();
        try (ResultSet rows = connection.getMetaData().getPrimaryKeys(null, schema, table)) {
            while (rows.next()) ordered.put(rows.getShort("KEY_SEQ"), rows.getString("COLUMN_NAME"));
        }
        return new ArrayList<>(ordered.values());
    }

    private static long countRows(Connection connection, String schema, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery("SELECT COUNT(*) FROM " + qualified(schema, table))) {
            row.next();
            return row.getLong(1);
        }
    }

    private static Map<String, ColumnInfo> byNormalizedName(List<ColumnInfo> columns) {
        Map<String, ColumnInfo> result = new HashMap<>();
        for (ColumnInfo column : columns) result.put(normalize(column.name), column);
        return result;
    }

    private static Collection<String> targetNamesForSource(Map<String, String> sourceTables,
                                                            Map<String, String> targetTables) {
        List<String> result = new ArrayList<>();
        for (String normalized : sourceTables.keySet()) result.add(targetTables.get(normalized));
        return result;
    }

    private static List<String> columnNames(List<ColumnInfo> columns) {
        List<String> names = new ArrayList<>();
        for (ColumnInfo column : columns) names.add(column.name);
        return names;
    }

    private static Set<String> normalized(Collection<String> values) {
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) result.add(normalize(value));
        return result;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String schemaOrDefault(Connection connection, String fallback) throws SQLException {
        String schema = connection.getSchema();
        return schema == null || schema.trim().isEmpty() ? fallback : schema;
    }

    private static String qualified(String schema, String table) {
        return quoted(schema) + "." + quoted(table);
    }

    static String quoted(String identifier) {
        if (identifier == null || identifier.isEmpty()) throw new IllegalArgumentException("标识符不能为空");
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static String joinQuoted(List<String> identifiers) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < identifiers.size(); i++) {
            if (i > 0) result.append(',');
            result.append(quoted(identifiers.get(i)));
        }
        return result.toString();
    }

    private static String orderBy(List<String> primaryKeys) {
        return primaryKeys.isEmpty() ? "" : " ORDER BY " + joinQuoted(primaryKeys);
    }

    private static boolean isPostgreSql(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgresql");
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", e);
        }
    }

    private static String hex(byte[] values) {
        StringBuilder result = new StringBuilder(values.length * 2);
        for (byte value : values) result.append(String.format("%02x", value));
        return result.toString();
    }

    private static class ColumnInfo {
        private String name;
        private boolean nullable;
        private String defaultValue;
        private boolean autoIncrement;
    }
}
