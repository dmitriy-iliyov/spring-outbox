package io.github.dmitriyiliyov.springoutbox.dlq;

import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class PostgreSqlOutboxDlqRepository implements OutboxDlqRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgreSqlOutboxDlqRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    @Override
    public void saveBatch(List<OutboxDlqEvent> eventBatch) {
        String sql = """
            INSERT INTO outbox_dlq_events
            (id, status, dlq_status, event_type, payload_type, payload, retry_count, created_at, processed_at, failed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        List<Object[]> params = eventBatch.stream()
                .map(e -> new Object[]{
                        e.getId(),
                        e.getStatus().name(),
                        e.getDlqStatus().name(),
                        e.getEventType(),
                        e.getPayloadType(),
                        e.getPayload(),
                        e.getRetryCount(),
                        e.getCreatedAt(),
                        e.getProcessedAt(),
                        e.getFailedAt()
                })
                .toList();
        jdbcTemplate.batchUpdate(sql, params);
    }

    @Transactional(readOnly = true)
    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM outbox_dlq_events";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    @Transactional(readOnly = true)
    @Override
    public long countByStatus(DlqStatus status) {
        String sql = "SELECT COUNT(*) FROM outbox_dlq_events WHERE dlq_status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status.name());
        return count == null ? 0 : count;
    }

    @Transactional(readOnly = true)
    @Override
    public long countByEventTypeAndStatus(String eventType, DlqStatus status) {
        String sql = "SELECT COUNT(*) FROM outbox_dlq_events WHERE event_type = ? AND dlq_status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, eventType, status.name());
        return count == null ? 0 : count;
    }

    @Transactional
    @Override
    public List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchSize) {
        String sql = """
            SELECT id, status, dlq_status, event_type, payload_type, payload, retry_count, created_at, processed_at, failed_at
            FROM outbox_dlq_events
            WHERE dlq_status = ?
            LIMIT ?
            FOR UPDATE SKIP LOCKED
        """;
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, batchSize);
                },
                (rs, rowNum) -> toEvent(rs)
        );
    }

    @Transactional(readOnly = true)
    @Override
    public List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchNumber, int batchSize) {
        String sql = """
            SELECT id, status, dlq_status, event_type, payload_type, payload, retry_count, created_at, processed_at, failed_at
            FROM outbox_dlq_events
            WHERE dlq_status = ?
            OFFSET ?
            LIMIT ?
        """;
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, (batchNumber - 1) * batchSize);
                    ps.setInt(3, batchSize);
                },
                (rs, rowNum) -> toEvent(rs)
        );
    }

    @Transactional
    @Override
    public void updateStatus(UUID id, DlqStatus status) {
        String sql = "UPDATE outbox_dlq_events SET dlq_status = ? WHERE id = ?";
        jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setObject(2, id);
                }
        );
    }

    @Transactional
    @Override
    public void updateBatchStatus(Set<UUID> ids, DlqStatus status) {
        if (!validateIds(ids)) return;
        String sql = "UPDATE outbox_dlq_events SET status = ? WHERE id IN (" + generatePlaceholders(ids) + ")";
        List<Object> params = new ArrayList<>();
        params.add(status.name());
        params.addAll(ids);
        jdbcTemplate.update(sql, params.toArray());
    }

    @Transactional
    @Override
    public void deleteBatch(Set<UUID> ids) {
        if (!validateIds(ids)) return;
        String sql = "DELETE FROM outbox_dlq_events WHERE id IN (" + generatePlaceholders(ids) + ")";
        List<Object> params = new ArrayList<>();
        params.addAll(ids);
        jdbcTemplate.update(sql, params.toArray());
    }

    private boolean validateIds(Set<UUID> ids) {
        Objects.requireNonNull(ids, "ids cannot be null");
        if (ids.isEmpty()) {
            return false;
        }
        if (ids.size() > 100) {
            throw new IllegalArgumentException("Batch size too large: " + ids.size() + ", max 100");
        }
        return true;
    }

    private String generatePlaceholders(Set<UUID> ids) {
        return ids.stream().map(id -> "?").collect(Collectors.joining(", "));
    }

    private OutboxDlqEvent toEvent(ResultSet rs) throws SQLException {
        return new OutboxDlqEvent(
                rs.getObject("id", UUID.class),
                EventStatus.fromString(rs.getString("status")),
                rs.getString("event_type"),
                rs.getString("payload_type"),
                rs.getString("payload"),
                rs.getInt("retry_count"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("processed_at") != null
                        ? rs.getTimestamp("processed_at").toInstant() : null,
                rs.getTimestamp("failed_at") != null
                        ? rs.getTimestamp("failed_at").toInstant() : null,
                DlqStatus.fromString(rs.getString("dlq_status"))
        );
    }
}
