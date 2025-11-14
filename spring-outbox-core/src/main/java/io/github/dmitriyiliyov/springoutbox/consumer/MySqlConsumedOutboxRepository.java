package io.github.dmitriyiliyov.springoutbox.consumer;

import io.github.dmitriyiliyov.springoutbox.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public class MySqlConsumedOutboxRepository implements ConsumedOutboxRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final SqlIdHelper idHelper;

    public MySqlConsumedOutboxRepository(JdbcTemplate jdbcTemplate, SqlIdHelper idHelper) {
        this.jdbcTemplate = jdbcTemplate;
        this.idHelper = idHelper;
    }

    @Transactional
    @Override
    public int saveIfAbsent(UUID id) {
        String sql = """
            INSERT IGNORE INTO outbox_consumed_events (id, consumed_at)
            VALUES(?, ?)
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    idHelper.setIdToPs(ps, 1, id);
                    ps.setTimestamp(2, Timestamp.from(Instant.now()));
                }
        );
    }

    @Transactional
    @Override
    public void deleteBatchByThreshold(Instant threshold, int batchSize) {
        String sql = """
            DELETE FROM outbox_consumed_events
            WHERE id IN (
                SELECT id FROM(
                    SELECT id 
                    FROM outbox_consumed_events 
                    WHERE consumed_at < ?
                    ORDER BY consumed_at
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                ) AS to_delete
            )
        """;
        jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setTimestamp(1, Timestamp.from(threshold));
                    ps.setInt(2, batchSize);
                }
        );
    }
}
