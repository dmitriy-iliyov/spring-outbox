package io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import org.springframework.jdbc.core.JdbcTemplate;

public class MultiSqlDialectOutboxDlqMetricsRepository implements OutboxDlqMetricsRepository {

    private final JdbcTemplate jdbcTemplate;

    public MultiSqlDialectOutboxDlqMetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM outbox_dlq_events";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public long countByStatus(DlqStatus status) {
        String sql = "SELECT COUNT(*) FROM outbox_dlq_events WHERE dlq_status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status.name());
        return count == null ? 0 : count;
    }

    @Override
    public long countByEventTypeAndStatus(String eventType, DlqStatus status) {
        String sql = "SELECT COUNT(*) FROM outbox_dlq_events WHERE event_type = ? AND dlq_status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, eventType, status.name());
        return count == null ? 0 : count;
    }
}
