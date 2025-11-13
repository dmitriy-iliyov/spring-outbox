package io.github.dmitriyiliyov.springoutbox.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.publisher.utils.ResultSetMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * PostgreSQL-specific implementation of {@link OutboxDlqRepository}.
 * <ul>
 *
 *     <li>uses the PostgreSQL-specific clause
 *     <code>FOR UPDATE SKIP LOCKED</code> (available since PostgreSQL 9.5) in {@link #findBatchByStatus(DlqStatus, int)}
 *     to allow multiple service instances to process outbox DLQ batches in parallel without conflicting on the same rows</li>
 *
 *     <li>performs batched deletion of processed events using a CTE
 *     (<code>WITH ... AS</code>), ensuring predictable load on the database.</li>
 *
 * </ul>
 */
public class PostgreSqlOutboxDlqRepository extends AbstractOutboxDlqRepository {

    public PostgreSqlOutboxDlqRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Transactional
    @Override
    public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) {
        String sql = """
            WITH to_lock AS(
                UPDATE outbox_dlq_events
                    SET status = ?
                WHERE id IN(
                    SELECT id FROM outbox_dlq_events
                    WHERE dlq_status = ?
                    ORDER BY updated_at
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                )
                RETURNING id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at
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
                (rs, rowNum) -> ResultSetMapper.toDlqEvent(rs)
        );
    }

    @Transactional
    @Override
    public List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchSize) {
        String sql = """
            SELECT id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at
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
                (rs, rowNum) -> ResultSetMapper.toDlqEvent(rs)
        );
    }

    @Override
    protected void setId(PreparedStatement ps, int parameterIndex, UUID id) throws SQLException {
        ps.setObject(parameterIndex, id);
    }
}
