package io.github.dmitriyiliyov.springoutbox.core.consumer;

import io.github.dmitriyiliyov.springoutbox.core.utils.RepositoryUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PostgreSqlConsumedOutboxRepository implements ConsumedOutboxRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final Clock clock;

    public PostgreSqlConsumedOutboxRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public int saveIfAbsent(UUID id) {
        String sql = """
            INSERT INTO outbox_consumed_events (id, consumed_at)
            VALUES(?, ?)
            ON CONFLICT (id) DO NOTHING
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setObject(1, id);
                    ps.setTimestamp(2, Timestamp.from(clock.instant()));
                }
        );
    }

    @Override
    public Set<UUID> saveIfAbsent(Set<UUID> ids) {
        if (!RepositoryUtils.isIdsValid(ids)) {
            return Collections.emptySet();
        }
        String sql = """
                INSERT INTO outbox_consumed_events (id, consumed_at) 
                VALUES %s 
                ON CONFLICT (id) DO NOTHING
                RETURNING id
        """.formatted(RepositoryUtils.generateValuesPlaceholders(ids.size(), 2));
        Instant consumedAt = clock.instant();
        return new HashSet<>(
                jdbcTemplate.query(
                        sql,
                        ps -> {
                            int paramId = 1;
                            for (UUID id : ids) {
                                ps.setObject(paramId++, id);
                                ps.setTimestamp(paramId++, Timestamp.from(consumedAt));
                            }
                        },
                        (rs, rowNum) -> rs.getObject("id", UUID.class)
                )
        );
    }

    @Override
    public int deleteBatchByThreshold(Instant threshold, int batchSize) {
        String sql = """
            WITH to_delete AS (
                SELECT id 
                FROM outbox_consumed_events 
                WHERE consumed_at < ?
                ORDER BY consumed_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            DELETE FROM outbox_consumed_events
            WHERE id IN (SELECT id FROM to_delete)
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setTimestamp(1, Timestamp.from(threshold));
                    ps.setInt(2, batchSize);
                }
        );
    }
}
