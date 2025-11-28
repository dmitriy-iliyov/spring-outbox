package io.github.dmitriyiliyov.springoutbox.consumer;

import io.github.dmitriyiliyov.springoutbox.utils.BytesSqlIdHelper;
import io.github.dmitriyiliyov.springoutbox.utils.BytesSqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.utils.RepositoryUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class MySqlConsumedOutboxRepository implements ConsumedOutboxRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final BytesSqlIdHelper idHelper;
    protected final BytesSqlResultSetMapper mapper;

    public MySqlConsumedOutboxRepository(JdbcTemplate jdbcTemplate, BytesSqlIdHelper idHelper, BytesSqlResultSetMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.idHelper = idHelper;
        this.mapper = mapper;
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
    public Set<UUID> saveIfAbsent(Set<UUID> ids) {
        if (!RepositoryUtils.isIdsValid(ids)) {
            return Collections.emptySet();
        }
        String existsIdsSql = """
            SELECT id 
            FROM outbox_consumed_events
            WHERE id IN (%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        List<UUID> existsIds = jdbcTemplate.query(
                existsIdsSql,
                ps -> idHelper.setIdsToPs(ps, 1, ids),
                (rs, rowNum) -> mapper.fromBytesToUuid(rs.getBytes("id"))
        );
        Set<UUID> nonExistsIds = ids.stream()
                .filter(id -> !existsIds.contains(id))
                .collect(Collectors.toSet());
        if (!RepositoryUtils.isIdsValid(nonExistsIds)) {
            return Collections.emptySet();
        }
        String insertSql = """
            INSERT IGNORE INTO outbox_consumed_events(id, consumed_at)
            VALUES %s
        """.formatted(RepositoryUtils.generateValuesPlaceholders(nonExistsIds.size(), 2));
        Instant consumedAt = Instant.now();
        int updatedRows = jdbcTemplate.update(
                insertSql,
                ps -> {
                    int paramId = 1;
                    for (UUID id: nonExistsIds) {
                        ps.setBytes(paramId++, idHelper.uuidToBytes(id));
                        ps.setTimestamp(paramId++, Timestamp.from(consumedAt));
                    }
                }
        );
        if (nonExistsIds.size() != updatedRows) {
            throw new ConcurrentInsertException(nonExistsIds.size(), updatedRows, nonExistsIds);
        }
        return new HashSet<>(nonExistsIds);
    }

    @Transactional
    @Override
    public int deleteBatchByThreshold(Instant threshold, int batchSize) {
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
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setTimestamp(1, Timestamp.from(threshold));
                    ps.setInt(2, batchSize);
                }
        );
    }
}
