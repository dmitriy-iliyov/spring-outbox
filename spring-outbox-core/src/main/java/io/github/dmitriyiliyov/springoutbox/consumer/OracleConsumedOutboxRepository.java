package io.github.dmitriyiliyov.springoutbox.consumer;

import io.github.dmitriyiliyov.springoutbox.utils.BytesSqlIdHelper;
import io.github.dmitriyiliyov.springoutbox.utils.BytesSqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.utils.RepositoryUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    @Override
    public int saveIfAbsent(UUID id) {
        String sql = """
            INSERT INTO outbox_consumed_events (id, consumed_at)
            VALUES (?, ?)
        """;
        try {
            return jdbcTemplate.update(sql, ps -> {
                idHelper.setIdToPs(ps, 1, id);
                ps.setTimestamp(2, Timestamp.from(Instant.now()));
            });
        } catch (DuplicateKeyException e) {
            return 0;
        }
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
            INSERT INTO outbox_consumed_events(id, consumed_at)
            VALUES %s
        """.formatted(RepositoryUtils.generateValuesPlaceholders(nonExistsIds.size(), 2));
        Instant consumedAt = Instant.now();
        try {
            jdbcTemplate.update(
                    insertSql,
                    ps -> {
                        int paramId = 1;
                        for (UUID id: nonExistsIds) {
                            ps.setBytes(paramId++, idHelper.uuidToBytes(id));
                            ps.setTimestamp(paramId++, Timestamp.from(consumedAt));
                        }
                    }
            );
            return new HashSet<>(nonExistsIds);
        } catch (DuplicateKeyException e) {
            throw new ConcurrentInsertException(nonExistsIds.size(), 0, nonExistsIds);
        }
    }

    @Transactional
    @Override
    public void deleteBatchByThreshold(Instant threshold, int batchSize) {
        String selectSql = """
            SELECT id
            FROM consumed_outbox_events
            WHERE id IN(
                SELECT id
                FROM consumed_outbox_events
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
            return;
        }
        String deleteSql = """
            DELETE FROM consumed_outbox_events
            WHERE id IN(%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        jdbcTemplate.update(deleteSql, ps -> idHelper.setIdsToPs(ps, 1, ids));
    }
}
