package io.github.dmitriyiliyov.springoutbox.consumer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public class PostgreSqlConsumedOutboxRepository implements ConsumedOutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgreSqlConsumedOutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    @Override
    public int saveIfAbsent(UUID id) {
        String sql = """
            INSERT INTO outbox_consumed_events(id, consumed_at)
            VALUES(?, ?)
            ON CONFLICT DO NOTHING
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setObject(1, id);
                    ps.setTimestamp(2, Timestamp.from(Instant.now()));
                }
        );
    }

    @Transactional
    @Override
    public void deleteBatchByThreshold(Instant threshold, int batchSize) {
        String sql = """
            WITH to_delete AS(
                SELECT id 
                FROM outbox_consumed_events 
                WHERE consumed_at < ?
                ORDER BY consumed_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            DELETE FROM to_delete
            WHERE id IN (SELECT id FROM to_delete)
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
