package io.github.dmitriyiliyov.springoutbox.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.publisher.utils.ResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PostgreSQL-specific implementation of {@link OutboxDlqRepository}.
 */
public class PostgreSqlOutboxDlqRepository extends AbstractOutboxDlqRepository {

    public PostgreSqlOutboxDlqRepository(JdbcTemplate jdbcTemplate, SqlIdHelper idHelper, ResultSetMapper mapper) {
        super(jdbcTemplate, idHelper, mapper);
    }

    @Transactional
    @Override
    public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) {
        String sql = """
            WITH to_lock AS (
                SELECT id FROM outbox_dlq_events
                WHERE dlq_status = ?
                ORDER BY moved_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            UPDATE outbox_dlq_events
                SET status = ?
            WHERE id IN(SELECT id FROM to_lock)
            RETURNING id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at
        """;
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, lockStatus.name());
                    ps.setString(2, status.name());
                    ps.setInt(3, batchSize);
                },
                (rs, rowNum) -> mapper.toDlqEvent(rs)
        );
    }
}
