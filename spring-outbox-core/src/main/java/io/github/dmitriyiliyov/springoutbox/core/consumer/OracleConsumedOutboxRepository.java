package io.github.dmitriyiliyov.springoutbox.core.consumer;

import io.github.dmitriyiliyov.springoutbox.core.utils.BytesSqlIdHelper;
import io.github.dmitriyiliyov.springoutbox.core.utils.BytesSqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.RepositoryUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class OracleConsumedOutboxRepository implements ConsumedOutboxRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final BytesSqlIdHelper idHelper;
    protected final BytesSqlResultSetMapper mapper;

    public OracleConsumedOutboxRepository(JdbcTemplate jdbcTemplate, BytesSqlIdHelper idHelper, BytesSqlResultSetMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.idHelper = idHelper;
        this.mapper = mapper;
    }

    @Override
    public int saveIfAbsent(UUID id) {
        String sql = """
            MERGE INTO outbox_consumed_events t
            USING (
                SELECT ? AS id, ? AS consumed_at FROM dual
            ) v_t
            ON (t.id = v_t.id)
            WHEN NOT MATCHED THEN
                INSERT (id, consumed_at)
                VALUES (v_t.id, v_t.consumed_at);
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    idHelper.setIdToPs(ps, 1, id);
                    ps.setTimestamp(2, Timestamp.from(Instant.now()));
                }
        );
    }

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
            INSERT /*+ IGNORE_ROW_ON_DUPKEY_INDEX(outbox_consumed_events (id)) */
            INTO outbox_consumed_events (id, consumed_at)
            VALUES (?, ?)
        """;
        Instant consumedAt = Instant.now();
        int [][] batchUpdateResult = jdbcTemplate.batchUpdate(
                insertSql,
                nonExistsIds,
                nonExistsIds.size(),
                (ps, id) -> {
                    ps.setBytes(1, idHelper.uuidToBytes(id));
                    ps.setTimestamp(2, Timestamp.from(consumedAt));
                }
        );
        int updRowsCount = Arrays.stream(batchUpdateResult)
                .flatMapToInt(Arrays::stream)
                .sum();
        if (nonExistsIds.size() != updRowsCount) {
            throw new ConcurrentInsertException(nonExistsIds.size(), updRowsCount, nonExistsIds);
        }
        return nonExistsIds;
    }

    @Override
    public int deleteBatchByThreshold(Instant threshold, int batchSize) {
        String selectSql = """
            SELECT id
            FROM outbox_consumed_events
            WHERE id IN(
                SELECT id
                FROM outbox_consumed_events
                WHERE consumed_at <= ?
                FETCH FIRST ? ROWS ONLY
            )
            FOR UPDATE SKIP LOCKED
        """;
        Set<UUID> ids = new HashSet<>(
                jdbcTemplate.query(
                        selectSql,
                        ps -> {
                            ps.setTimestamp(1, Timestamp.from(threshold));
                            ps.setInt(2, batchSize);
                        },
                        (rs, rowNum) -> mapper.fromBytesToUuid(rs.getBytes("id"))
                )
        );
        if (!RepositoryUtils.isIdsValid(ids)) {
            return 0;
        }
        String deleteSql = """
            DELETE FROM outbox_consumed_events
            WHERE id IN(%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        return jdbcTemplate.update(deleteSql, ps -> idHelper.setIdsToPs(ps, 1, ids));
    }
}
