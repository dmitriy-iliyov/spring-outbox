package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class PostgreSqlOutboxRepository implements OutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgreSqlOutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void save(OutboxEvent event) {
        String sql = """
            INSERT INTO outbox_events 
            (id, status, event_type, payload_type, payload, retry_count, created_at, processed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                event.getId(),
                event.getStatus().name(),
                event.getEventType(),
                event.getPayloadType(),
                event.getPayload(),
                event.getRetryCount(),
                event.getCreatedAt(),
                event.getProcessedAt()
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void saveBatch(List<OutboxEvent> eventBatch) {
        String sql = """
            INSERT INTO outbox_events 
            (id, status, event_type, payload_type, payload, retry_count, created_at, processed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        List<Object[]> params = eventBatch.stream()
                .map(e ->
                        new Object[]{
                                e.getId(),
                                e.getStatus().name(),
                                e.getEventType(),
                                e.getPayloadType(),
                                e.getPayload(),
                                e.getRetryCount(),
                                e.getCreatedAt(),
                                e.getProcessedAt()
                })
                .toList();
        jdbcTemplate.batchUpdate(sql, params);
    }

    @Transactional
    @Override
    public List<OutboxEvent> findBatchByEventTypeAndStatus(String eventType, OutboxStatus status, int batchSize) {
        String sql = """
            SELECT id, status, event_type, payload_type, payload, retry_count, created_at, processed_at 
            FROM outbox_events
            WHERE event_type = ? AND status = ?
            ORDER BY created_at
            LIMIT ?
            FOR UPDATE SKIP LOCKED
        """;
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, eventType);
                    ps.setString(2, status.name());
                    ps.setInt(3, batchSize);
                },
                (rs, rowNum) -> toEvent(rs)
        );
    }

    @Transactional
    @Override
    public void updateBatchStatus(Set<UUID> ids, OutboxStatus status) {
        if (!validateIds(ids)) return;
        if (status.equals(OutboxStatus.FAILED)) {
            throw new IllegalArgumentException("Use updateBatchStatusWithRetry() for FAILED batch");
        }
        String placeholders = generatePlaceholders(ids);
        String sql;
        List<Object> params = new ArrayList<>();
        if (status.equals(OutboxStatus.PROCESSED)) {
            sql = "UPDATE outbox_events SET status = ?, processed_at = ? WHERE id IN (" + placeholders + ")";
            params.add(status.name());
            params.add(Timestamp.from(Instant.now()));
            params.addAll(ids);
        } else {
            sql = "UPDATE outbox_events SET status = ? WHERE id IN (" + placeholders + ")";
            params.add(status.name());
            params.addAll(ids);
        }
        jdbcTemplate.update(sql, params.toArray());
    }

    @Transactional
    @Override
    public void incrementRetryCountOrSetFailed(Set<UUID> ids, int maxRetryCount) {
        if (!validateIds(ids)) return;
        String sql = """
                UPDATE outbox_events 
                SET 
                    retry_count = CASE WHEN retry_count < ? THEN retry_count + 1 ELSE retry_count END,
                    status = CASE WHEN retry_count + 1 < ? THEN ? ELSE ? END
                WHERE id IN (%s)
            """.formatted(generatePlaceholders(ids));
        List<Object> params = new ArrayList<>();
        params.add(maxRetryCount);
        params.add(maxRetryCount);
        params.add(OutboxStatus.PENDING.name());
        params.add(OutboxStatus.FAILED.name());
        params.addAll(ids);
        jdbcTemplate.update(sql, params.toArray());
    }

    @Transactional
    @Override
    public void deleteBatchByProcessedAfterThreshold(Instant threshold, int batchSize) {
        String sql = """
            WITH to_delete AS(
                SELECT id FROM outbox_events 
                WHERE processed_at < ?
                ORDER BY processed_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )   
            DELETE FROM outbox_events 
            WHERE id IN (SELECT id FROM to_delete)
        """;
        List<Object> params = new ArrayList<>();
        params.add(threshold);
        params.add(batchSize);
        jdbcTemplate.update(sql, params.toArray());
    }

    private boolean validateIds(Set<UUID> ids) {
        Objects.requireNonNull(ids, "ids cannot be null");
        if (ids.isEmpty()) {
            return false;
        }
        if (ids.size() > 100) {
            throw new IllegalArgumentException("Batch size too large: " + ids.size() + ", max 1000");
        }
        return true;
    }

    private String generatePlaceholders(Set<UUID> ids) {
        return ids.stream().map(id -> "?").collect(Collectors.joining(", "));
    }

    private OutboxEvent toEvent(ResultSet rs) throws SQLException {
        return new OutboxEvent(
                rs.getObject("id", UUID.class),
                OutboxStatus.fromString(rs.getString("status")),
                rs.getString("event_type"),
                rs.getString("payload_type"),
                rs.getString("payload"),
                rs.getInt("retry_count"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("processed_at") != null
                        ? rs.getTimestamp("processed_at").toInstant() : null
        );
    }
}
