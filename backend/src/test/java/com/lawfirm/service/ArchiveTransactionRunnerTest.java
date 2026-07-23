package com.lawfirm.service;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArchiveTransactionRunnerTest {

    @Test
    void failedArchiveStepRollsBackBeforeFailureStatusCanBeRecordedSeparately() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:archive_tx;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table archive_tx_test (id bigint primary key, status varchar(20))");
        ArchiveTransactionRunner runner = new ArchiveTransactionRunner(
                new DataSourceTransactionManager(dataSource));

        assertThrows(IllegalStateException.class, () -> runner.execute(() -> {
            jdbc.update("insert into archive_tx_test (id, status) values (1, 'ARCHIVED')");
            throw new IllegalStateException("worker failed after case mutation");
        }));
        assertEquals(0, jdbc.queryForObject("select count(*) from archive_tx_test", Integer.class));

        runner.execute(() -> {
            jdbc.update("insert into archive_tx_test (id, status) values (1, 'FAILED')");
            return null;
        });
        assertEquals("FAILED", jdbc.queryForObject(
                "select status from archive_tx_test where id = 1", String.class));
    }
}
