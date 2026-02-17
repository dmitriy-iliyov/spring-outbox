package io.github.dmitriyiliyov.springoutbox.metrics.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import org.springframework.jdbc.core.JdbcTemplate;

public class MultiSqlDialectOutboxMetricsRepository implements OutboxMetricsRepository {

    private final JdbcTemplate jdbcTemplate;

    public MultiSqlDialectOutboxMetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM outbox_events";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public long countByStatus(EventStatus status) {
        String sql = "SELECT COUNT(*) FROM outbox_events WHERE status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status.name());
        return count == null ? 0 : count;
    }

    @Override
    public long countByEventTypeAndStatus(String eventType, EventStatus status) {
        String sql = "SELECT COUNT(*) FROM outbox_events WHERE event_type = ? AND status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, eventType, status.name());
        return count == null ? 0 : count;
    }
}
