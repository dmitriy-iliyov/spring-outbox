package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.utils.RepositoryUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * PostgreSQL-specific implementation of {@link OutboxRepository}.
 * <ul>
 *     <li> uses {@link Transactional @Transactional} with
 *     {@link Propagation#MANDATORY}, ensuring that all outbox entries are persisted atomically
 *     as part of the business transaction.</li>
 *
 *     <li>uses the PostgreSQL-specific clause
 *     <code>FOR UPDATE SKIP LOCKED</code> (available since PostgreSQL 9.5) to allow multiple service instances
 *     to process outbox batches in parallel without conflicting on the same rows.</li>
 *
 *     <li>performs batched deletion of processed events using a CTE
 *     (<code>WITH ... DELETE</code>), ensuring predictable load on the database.</li>
 *
 *     <li> provides a mechanism for incrementing retry counters and marking permanently failed events.</li>
 * </ul>
 */
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
            (id, status, event_type, payload_type, payload, retry_count, created_at, processed_at, failed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                event.getProcessedAt(),
                event.getFailedAt()
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void saveBatch(List<OutboxEvent> eventBatch) {
        String sql = """
            INSERT INTO outbox_events 
            (id, status, event_type, payload_type, payload, retry_count, created_at, processed_at, failed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                                e.getProcessedAt(),
                                e.getFailedAt()
                })
                .toList();
        jdbcTemplate.batchUpdate(sql, params);
    }

    @Transactional(readOnly = true)
    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM outbox_events";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    @Transactional(readOnly = true)
    @Override
    public long countByStatus(EventStatus status) {
        String sql = "SELECT COUNT(*) FROM outbox_events WHERE status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status.name());
        return count == null ? 0 : count;
    }

    @Transactional(readOnly = true)
    @Override
    public long countByEventTypeAndStatus(String eventType, EventStatus status) {
        String sql = "SELECT COUNT(*) FROM outbox_events WHERE event_type = ? AND status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, eventType, status.name());
        return count == null ? 0 : count;
    }

    @Transactional
    @Override
    public List<OutboxEvent> findBatchByStatus(EventStatus status, int batchSize, String orderBy) {
        List<String> allowedOrderBy = List.of("created_at", "processed_at", "failed_at");
        if (!allowedOrderBy.contains(orderBy)) {
            throw new IllegalArgumentException("Not allowed orderBy");
        }
        String sql = """
            SELECT id, status, event_type, payload_type, payload, retry_count, created_at, processed_at, failed_at
            FROM outbox_events
            WHERE status = ?
            ORDER BY %s
            LIMIT ?
            FOR UPDATE SKIP LOCKED
        """.formatted(orderBy);
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, batchSize);
                },
                (rs, rowNum) -> toEvent(rs)
        );
    }

    @Transactional
    @Override
    public List<OutboxEvent> findBatchByEventTypeAndStatus(String eventType, EventStatus status, int batchSize) {
        String sql = """
            SELECT id, status, event_type, payload_type, payload, retry_count, created_at, processed_at, failed_at 
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
    public void updateBatchStatus(Set<UUID> ids, EventStatus status) {
        if (!RepositoryUtils.validateIds(ids)) return;
        if (status.equals(EventStatus.FAILED)) {
            throw new IllegalArgumentException("Use incrementRetryCountOrSetFailed() for FAILED batch");
        }
        String placeholders = RepositoryUtils.generatePlaceholders(ids);
        String sql;
        List<Object> params = new ArrayList<>();
        if (status.equals(EventStatus.PROCESSED)) {
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
        if (!RepositoryUtils.validateIds(ids)) return;
        String sql = """
                UPDATE outbox_events 
                SET 
                    retry_count = CASE WHEN retry_count < ? THEN retry_count + 1 ELSE retry_count END,
                    status = CASE WHEN retry_count + 1 < ? THEN ? ELSE ? END
                    failed_at = CASE WHEN retry_count + 1 < ? THEN ? ELSE failed_at
                WHERE id IN (%s)
            """.formatted(RepositoryUtils.generatePlaceholders(ids));
        List<Object> params = new ArrayList<>();
        params.add(maxRetryCount);
        params.add(maxRetryCount);
        params.add(EventStatus.PENDING.name());
        params.add(EventStatus.FAILED.name());
        params.add(maxRetryCount);
        params.add(Instant.now());
        params.addAll(ids);
        jdbcTemplate.update(sql, params.toArray());
    }

    @Transactional
    @Override
    public void deleteBatch(Set<UUID> ids) {
        if (!RepositoryUtils.validateIds(ids)) return;
        String sql = "DELETE FROM outbox_events WHERE id IN (%s)".formatted(RepositoryUtils.generatePlaceholders(ids));
        jdbcTemplate.update(sql, ids.toArray());
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

    private OutboxEvent toEvent(ResultSet rs) throws SQLException {
        Timestamp processedAt = rs.getTimestamp("processed_at");
        Timestamp failedAt = rs.getTimestamp("failed_at");
        return new OutboxEvent(
                rs.getObject("id", UUID.class),
                EventStatus.fromString(rs.getString("status")),
                rs.getString("event_type"),
                rs.getString("payload_type"),
                rs.getString("payload"),
                rs.getInt("retry_count"),
                rs.getTimestamp("created_at").toInstant(),
                processedAt != null ? processedAt.toInstant() : null,
                failedAt != null ? failedAt.toInstant() : null
        );
    }
}
