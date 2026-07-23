package com.lawfirm.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationDatabaseCopierTest {

    @TempDir
    Path tempDir;

    @Test
    void copiesRelatedTablesAndVerifiesPrimaryKeys() throws Exception {
        try (Connection source = DriverManager.getConnection("jdbc:h2:mem:migration_source;DB_CLOSE_DELAY=-1");
             Connection target = DriverManager.getConnection("jdbc:h2:mem:migration_target;DB_CLOSE_DELAY=-1")) {
            createSourceSchema(source);
            createTargetSchema(target);
            try (Statement statement = source.createStatement()) {
                statement.execute("INSERT INTO parent(id, name, notes, payload) VALUES "
                        + "(1, 'Alpha', 'long text', X'010203'), (2, 'Beta', NULL, X'FF')");
                statement.execute("INSERT INTO child(id, parent_id, title) VALUES (10, 1, 'First')");
            }

            MigrationReport report = baseReport();
            new MigrationDatabaseCopier(source, target).migrate(report);

            assertEquals("SUCCESS", report.getStatus());
            assertEquals(3, report.getSourceRowTotal());
            assertEquals(3, report.getTargetRowTotal());
            assertEquals(Arrays.asList("parent", "child"), tableNames(report));
            assertTrue(report.getTables().stream().allMatch(table -> "VERIFIED".equals(table.getStatus())));
            assertTrue(report.getTables().stream().allMatch(table ->
                    table.getSourcePrimaryKeySha256().equals(table.getTargetPrimaryKeySha256())));

            try (Statement statement = target.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT \"name\", \"notes\", \"payload\" FROM \"parent\" WHERE \"id\"=1")) {
                rows.next();
                assertEquals("Alpha", rows.getString(1));
                assertEquals("long text", rows.getString(2));
                assertArrayEquals(new byte[]{1, 2, 3}, rows.getBytes(3));
            }
        }
    }

    @Test
    void rejectsNonEmptyTargetWithoutChangingIt() throws Exception {
        try (Connection source = DriverManager.getConnection("jdbc:h2:mem:migration_source_blocked;DB_CLOSE_DELAY=-1");
             Connection target = DriverManager.getConnection("jdbc:h2:mem:migration_target_blocked;DB_CLOSE_DELAY=-1")) {
            createSourceSchema(source);
            createTargetSchema(target);
            source.createStatement().execute("INSERT INTO parent(id, name) VALUES (1, 'Source')");
            target.createStatement().execute("INSERT INTO \"parent\"(\"id\", \"name\") VALUES (9, 'Existing')");

            assertThrows(IllegalStateException.class,
                    () -> new MigrationDatabaseCopier(source, target).migrate(baseReport()));
            try (ResultSet row = target.createStatement().executeQuery(
                    "SELECT \"name\" FROM \"parent\" WHERE \"id\"=9")) {
                row.next();
                assertEquals("Existing", row.getString(1));
            }
        }
    }

    @Test
    void rollsBackAllCopiedRowsWhenLaterTableFails() throws Exception {
        try (Connection source = DriverManager.getConnection("jdbc:h2:mem:migration_source_rollback;DB_CLOSE_DELAY=-1");
             Connection target = DriverManager.getConnection("jdbc:h2:mem:migration_target_rollback;DB_CLOSE_DELAY=-1")) {
            createSourceSchema(source);
            createFailingTargetSchema(target);
            source.createStatement().execute("INSERT INTO parent(id, name) VALUES (1, 'Source')");
            source.createStatement().execute(
                    "INSERT INTO child(id, parent_id, title) VALUES (10, 1, 'Title exceeds target')");

            assertThrows(Exception.class,
                    () -> new MigrationDatabaseCopier(source, target).migrate(baseReport()));

            try (ResultSet row = target.createStatement().executeQuery("SELECT COUNT(*) FROM \"parent\"")) {
                row.next();
                assertEquals(0, row.getLong(1));
            }
        }
    }

    @Test
    void planIsBlockedByAdditionalNonEmptyTargetTable() throws Exception {
        try (Connection source = DriverManager.getConnection("jdbc:h2:mem:migration_source_plan;DB_CLOSE_DELAY=-1");
             Connection target = DriverManager.getConnection("jdbc:h2:mem:migration_target_plan;DB_CLOSE_DELAY=-1")) {
            createSourceSchema(source);
            createTargetSchema(target);
            target.createStatement().execute("CREATE TABLE \"unexpected\"(\"id\" BIGINT PRIMARY KEY)");
            target.createStatement().execute("INSERT INTO \"unexpected\"(\"id\") VALUES (1)");

            MigrationReport report = new MigrationDatabaseCopier(source, target).plan(baseReport());

            assertEquals("BLOCKED", report.getStatus());
            assertTrue(report.getWarnings().stream().anyMatch(value -> value.contains("unexpected=1")));
        }
    }

    @Test
    void migratesPopulatedCaseVariantAndWarnsAboutEmptyDuplicate() throws Exception {
        try (Connection source = DriverManager.getConnection("jdbc:h2:mem:migration_source_case;DB_CLOSE_DELAY=-1");
             Connection target = DriverManager.getConnection("jdbc:h2:mem:migration_target_case;DB_CLOSE_DELAY=-1")) {
            source.createStatement().execute("CREATE TABLE DUPLICATE_NAME(id BIGINT PRIMARY KEY)");
            source.createStatement().execute("CREATE TABLE \"duplicate_name\"(\"id\" BIGINT PRIMARY KEY)");
            source.createStatement().execute("INSERT INTO \"duplicate_name\"(\"id\") VALUES (7)");
            target.createStatement().execute("CREATE TABLE \"duplicate_name\"(\"id\" BIGINT PRIMARY KEY)");

            MigrationReport report = new MigrationDatabaseCopier(source, target).migrate(baseReport());

            assertEquals("SUCCESS", report.getStatus());
            assertTrue(report.getWarnings().stream().anyMatch(value -> value.contains("DUPLICATE_NAME")));
            try (ResultSet row = target.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM \"duplicate_name\" WHERE \"id\"=7")) {
                row.next();
                assertEquals(1, row.getLong(1));
            }
        }
    }

    @Test
    void rejectsCaseVariantsWhenBothContainRows() throws Exception {
        try (Connection source = DriverManager.getConnection("jdbc:h2:mem:migration_source_case_blocked;DB_CLOSE_DELAY=-1");
             Connection target = DriverManager.getConnection("jdbc:h2:mem:migration_target_case_blocked;DB_CLOSE_DELAY=-1")) {
            source.createStatement().execute("CREATE TABLE DUPLICATE_NAME(id BIGINT PRIMARY KEY)");
            source.createStatement().execute("CREATE TABLE \"duplicate_name\"(\"id\" BIGINT PRIMARY KEY)");
            source.createStatement().execute("INSERT INTO DUPLICATE_NAME(id) VALUES (1)");
            source.createStatement().execute("INSERT INTO \"duplicate_name\"(\"id\") VALUES (2)");
            target.createStatement().execute("CREATE TABLE \"duplicate_name\"(\"id\" BIGINT PRIMARY KEY)");

            assertThrows(IllegalStateException.class,
                    () -> new MigrationDatabaseCopier(source, target).plan(baseReport()));
        }
    }

    @Test
    void ordersDependenciesBeforeChildrenAndRejectsCycles() {
        Map<String, Set<String>> dependencies = new LinkedHashMap<>();
        dependencies.put("child", new LinkedHashSet<>(Collections.singletonList("parent")));
        dependencies.put("parent", Collections.emptySet());
        assertEquals(Arrays.asList("parent", "child"),
                MigrationDatabaseCopier.dependencyOrder(dependencies));

        dependencies.clear();
        dependencies.put("a", new LinkedHashSet<>(Collections.singletonList("b")));
        dependencies.put("b", new LinkedHashSet<>(Collections.singletonList("a")));
        assertThrows(IllegalStateException.class,
                () -> MigrationDatabaseCopier.dependencyOrder(dependencies));
    }

    @Test
    void validatesTargetDatabaseNameAndQuotesIdentifiers() {
        assertEquals("zgai_migration", H2ToPostgreSqlMigration.postgreSqlDatabaseName(
                "jdbc:postgresql://localhost:5432/zgai_migration?sslmode=disable"));
        assertThrows(IllegalArgumentException.class,
                () -> H2ToPostgreSqlMigration.postgreSqlDatabaseName("jdbc:postgresql://localhost/postgres"));
        assertEquals("\"user\"", MigrationDatabaseCopier.quoted("user"));
        assertEquals("\"a\"\"b\"", MigrationDatabaseCopier.quoted("a\"b"));
    }

    @Test
    void generatesSchemaWithApplicationNamingAndManagedConverters() throws Exception {
        String url = "jdbc:h2:mem:migration_generated_schema_" + System.nanoTime()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

        TargetSchemaGenerator.createH2CompatibilitySchema(url);

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            assertTrue(columnExists(connection, "ai_config", "system_prompt"));
            assertTrue(columnExists(connection, "user", "email"));
            assertTrue(columnExists(connection, "user", "phone"));
        }
    }

    @Test
    void validatesOfflineSourcePathReadOnlyModeAndActualDigest() throws Exception {
        Path basePath = tempDir.resolve("lawfirm-copy");
        Path dataFile = tempDir.resolve("lawfirm-copy.mv.db");
        Files.write(dataFile, "test".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String digest = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
        String readOnlyUrl = "jdbc:h2:file:" + basePath + ";MODE=MySQL;ACCESS_MODE_DATA=r";

        assertEquals(dataFile, MigrationSourceValidator.validateOfflineCopy(readOnlyUrl, digest));
        assertThrows(IllegalArgumentException.class,
                () -> MigrationSourceValidator.validateOfflineCopy(
                        "jdbc:h2:file:" + basePath + ";MODE=MySQL", digest));
        assertThrows(IllegalArgumentException.class,
                () -> MigrationSourceValidator.validateOfflineCopy(readOnlyUrl,
                        "0000000000000000000000000000000000000000000000000000000000000000"));
        assertThrows(IllegalArgumentException.class,
                () -> MigrationSourceValidator.validateOfflineCopy(
                        "jdbc:h2:file:relative-copy;ACCESS_MODE_DATA=r", digest));
    }

    private void createSourceSchema(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE parent(id BIGINT PRIMARY KEY, name VARCHAR(100) NOT NULL, "
                    + "notes CLOB, payload BINARY VARYING)");
            statement.execute("CREATE TABLE child(id BIGINT PRIMARY KEY, parent_id BIGINT NOT NULL, "
                    + "title VARCHAR(100), CONSTRAINT fk_parent FOREIGN KEY(parent_id) REFERENCES parent(id))");
        }
    }

    private void createTargetSchema(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE \"parent\"(\"id\" BIGINT PRIMARY KEY, "
                    + "\"name\" VARCHAR(100) NOT NULL, \"notes\" CLOB, \"payload\" BINARY VARYING)");
            statement.execute("CREATE TABLE \"child\"(\"id\" BIGINT PRIMARY KEY, \"parent_id\" BIGINT NOT NULL, "
                    + "\"title\" VARCHAR(100), CONSTRAINT \"fk_parent\" FOREIGN KEY(\"parent_id\") "
                    + "REFERENCES \"parent\"(\"id\"))");
        }
    }

    private void createFailingTargetSchema(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE \"parent\"(\"id\" BIGINT PRIMARY KEY, "
                    + "\"name\" VARCHAR(100) NOT NULL, \"notes\" CLOB, \"payload\" BINARY VARYING)");
            statement.execute("CREATE TABLE \"child\"(\"id\" BIGINT PRIMARY KEY, \"parent_id\" BIGINT NOT NULL, "
                    + "\"title\" VARCHAR(3), CONSTRAINT \"fk_parent\" FOREIGN KEY(\"parent_id\") "
                    + "REFERENCES \"parent\"(\"id\"))");
        }
    }

    private MigrationReport baseReport() {
        MigrationReport report = new MigrationReport();
        report.setMigrationId("00000000-0000-0000-0000-000000000001");
        report.setAction("EXECUTE");
        report.setStatus("RUNNING");
        report.setSourceSha256("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        report.setStartedAt("2026-07-23T00:00:00+08:00");
        return report;
    }

    private boolean columnExists(Connection connection, String table, String column) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(null, "public", table, column)) {
            return columns.next();
        }
    }

    private List<String> tableNames(MigrationReport report) {
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        for (MigrationReport.TableResult table : report.getTables()) names.add(table.getTargetTable().toLowerCase());
        return names;
    }
}
