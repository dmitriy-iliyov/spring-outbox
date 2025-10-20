package io.github.dmitriyiliyov.springoutbox.core.dlq;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PostgreSqlOutboxDlqRepository implements OutboxDlqRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgreSqlOutboxDlqRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    @Override
    public void saveBatch(List<OutboxDlqEvent> dlqEvents) {

    }

    @Transactional
    @Override
    public List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchSize) {
        return List.of();
    }

    @Transactional
    @Override
    public void updateBatchStatus(Set<UUID> ids, DlqStatus status) {

    }
}
