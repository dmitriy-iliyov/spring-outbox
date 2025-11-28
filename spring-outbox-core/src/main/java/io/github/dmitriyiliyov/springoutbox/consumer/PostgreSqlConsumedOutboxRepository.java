package io.github.dmitriyiliyov.springoutbox.consumer;

import io.github.dmitriyiliyov.springoutbox.utils.RepositoryUtils;
import io.github.dmitriyiliyov.springoutbox.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PostgreSqlConsumedOutboxRepository implements ConsumedOutboxRepository {

    private final JdbcTemplate jdbcTemplate;
    private final SqlIdHelper idHelper;

    public PostgreSqlConsumedOutboxRepository(JdbcTemplate jdbcTemplate, SqlIdHelper idHelper) {
        this.jdbcTemplate = jdbcTemplate;
        this.idHelper = idHelper;
    }

    @Transactional
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
                    ps.setTimestamp(2, Timestamp.from(Instant.now()));
                }
        );
    }

    @Transactional
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
        Instant consumedAt = Instant.now();
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

    @Transactional
    @Override
    public void deleteBatchByThreshold(Instant threshold, int batchSize) {
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
        jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setTimestamp(1, Timestamp.from(threshold));
                    ps.setInt(2, batchSize);
                }
        );
    }
}
