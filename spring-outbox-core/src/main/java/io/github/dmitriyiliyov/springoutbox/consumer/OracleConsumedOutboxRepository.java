package io.github.dmitriyiliyov.springoutbox.consumer;

import io.github.dmitriyiliyov.springoutbox.publisher.utils.BytesSqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.RepositoryUtils;
import io.github.dmitriyiliyov.springoutbox.utils.SqlIdHelper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class OracleConsumedOutboxRepository implements ConsumedOutboxRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final SqlIdHelper idHelper;
    protected final BytesSqlResultSetMapper mapper;

    public OracleConsumedOutboxRepository(JdbcTemplate jdbcTemplate, SqlIdHelper idHelper, BytesSqlResultSetMapper mapper) {
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
        if (!RepositoryUtils.validateIds(ids)) {
            return;
        }
        String deleteSql = """
            DELETE FROM consumed_outbox_events
            WHERE id IN(%s)
        """.formatted(RepositoryUtils.generatePlaceholders(ids));
        jdbcTemplate.update(deleteSql, idHelper.convertIdsToDbFormat(ids).toArray());
    }
}
