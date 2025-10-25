package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.utils.ResultSetMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * PostgreSQL-specific implementation of {@link OutboxRepository}.
 * <ul>
 *
 *     <li>uses the PostgreSQL-specific clause
 *     <code>FOR UPDATE SKIP LOCKED</code> (available since PostgreSQL 9.5) to allow multiple service instances
 *     to process outbox batches in parallel without conflicting on the same rows.</li>
 *
 *     <li>performs batched deletion of processed events using a CTE
 *     (<code>WITH ... AS</code>), ensuring predictable load on the database.</li>
 *
 * </ul>
 */
public class PostgreSqlOutboxRepository extends AbstractOutboxRepository {

    public PostgreSqlOutboxRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Transactional
    @Override
    public List<OutboxEvent> findAndLockBatchByEventTypeAndStatus(String eventType, EventStatus status, int batchSize,
                                                                  EventStatus lockStatus) {
        String sql = """
            WITH to_lock AS(
                UPDATE outbox_events
                    SET status = ?
                WHERE id IN(
                    SELECT id FROM outbox_events 
                    WHERE event_type = ? AND status = ?
                    ORDER BY created_at
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                )
                RETURNING id, status, event_type, payload_type, payload, retry_count, created_at, updated_at
            )
            SELECT * FROM to_lock
        """;
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, lockStatus.name());
                    ps.setString(2, eventType);
                    ps.setString(3, status.name());
                    ps.setInt(4, batchSize);
                },
                (rs, rowNum) -> ResultSetMapper.toEvent(rs)
        );
    }

    @Transactional
    @Override
    public List<OutboxEvent> findAndLockBatchByStatus(EventStatus status, int batchSize, EventStatus lockStatus) {
        String sql = """
            WITH to_lock AS(
                UPDATE outbox_events
                    SET status = ?
                WHERE id IN(
                    SELECT id FROM outbox_events
                    WHERE status = ?
                    ORDER BY updated_at
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                )
                RETURNING id, status, event_type, payload_type, payload, retry_count, created_at, updated_at
            )
            SELECT * FROM to_lock
        """;
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, lockStatus.name());
                    ps.setString(2, status.name());
                    ps.setInt(3, batchSize);
                },
                (rs, rowNum) -> ResultSetMapper.toEvent(rs)
        );
    }

    @Transactional
    @Override
    public void deleteBatchByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize) {
        String sql = """
            WITH to_delete AS(
                SELECT id FROM outbox_events 
                WHERE status = ? AND updated_at < ?
                ORDER BY updated_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            DELETE FROM outbox_events 
            WHERE id IN (SELECT id FROM to_delete)
        """;
        jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setTimestamp(2, Timestamp.from(threshold));
                    ps.setInt(3, batchSize);
                }
        );
    }

    @Transactional
    @Override
    public int updateBatchStatusByStatus(EventStatus status, int batchSize, EventStatus newStatus) {
        String sql = """
            WITH to_update AS(
                SELECT id FROM outbox_events
                WHERE status = ?
                ORDER BY updated_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            UPDATE outbox_events
                SET status = ?
            WHERE id IN (SELECT id FROM to_update)
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, batchSize);
                    ps.setString(3, newStatus.name());
                }
        );
    }
}
