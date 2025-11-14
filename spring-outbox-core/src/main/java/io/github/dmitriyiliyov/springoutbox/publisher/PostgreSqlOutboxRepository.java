package io.github.dmitriyiliyov.springoutbox.publisher;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.ResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
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

    protected final ResultSetMapper mapper;

    public PostgreSqlOutboxRepository(JdbcTemplate jdbcTemplate, SqlIdHelper idHelper, ResultSetMapper mapper) {
        super(jdbcTemplate, idHelper);
        this.mapper = mapper;
    }

    @Transactional
    @Override
    public List<OutboxEvent> findAndLockBatchByEventTypeAndStatus(String eventType, EventStatus status, int batchSize,
                                                                  EventStatus lockStatus) {
        String sql = """
            UPDATE outbox_events
                SET status = ?, updated_at = ?
            WHERE id IN(
                SELECT id FROM outbox_events
                WHERE event_type = ? AND status = ? AND next_retry_at <= ?
                ORDER BY next_retry_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            RETURNING id, status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at
        """;
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, lockStatus.name());
                    ps.setTimestamp(2, Timestamp.from(Instant.now()));
                    ps.setString(3, eventType);
                    ps.setString(4, status.name());
                    ps.setTimestamp(5, Timestamp.from(Instant.now()));
                    ps.setInt(6, batchSize);
                },
                (rs, rowNum) -> mapper.toEvent(rs)
        );
    }

    @Transactional
    @Override
    public List<OutboxEvent> findAndLockBatchByStatus(EventStatus status, int batchSize, EventStatus lockStatus) {
        String sql = """
            UPDATE outbox_events
                SET status = ?, updated_at = ?
            WHERE id IN(
                SELECT id FROM outbox_events
                WHERE status = ?
                ORDER BY updated_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            RETURNING id, status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at
        """;
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, lockStatus.name());
                    ps.setTimestamp(2, Timestamp.from(Instant.now()));
                    ps.setString(3, status.name());
                    ps.setInt(4, batchSize);
                },
                (rs, rowNum) -> mapper.toEvent(rs)
        );
    }

    @Transactional
    @Override
    public int updateBatchStatusByStatus(EventStatus status, int batchSize, EventStatus newStatus) {
        String sql = """
            UPDATE outbox_events
                SET status = ?, updated_at = ?
            WHERE id IN (
                SELECT id FROM outbox_events
                WHERE status = ?
                ORDER BY updated_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, newStatus.name());
                    ps.setTimestamp(2, Timestamp.from(Instant.now()));
                    ps.setString(3, status.name());
                    ps.setInt(4, batchSize);
                }
        );
    }

    @Transactional
    @Override
    public void deleteBatchByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize) {
        String sql = """
            DELETE FROM outbox_events 
            WHERE id IN (
                SELECT id FROM outbox_events 
                WHERE status = ? AND updated_at < ?
                ORDER BY updated_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
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
}
