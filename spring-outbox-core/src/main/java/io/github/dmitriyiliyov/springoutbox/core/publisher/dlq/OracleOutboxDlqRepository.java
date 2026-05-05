package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.utils.BytesResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.BytesSqlIdHelper;
import io.github.dmitriyiliyov.springoutbox.core.utils.RepositoryUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class OracleOutboxDlqRepository extends AbstractOutboxDlqRepository {

    private final BytesResultSetMapper localMapper;
    private final Clock clock;

    public OracleOutboxDlqRepository(JdbcTemplate jdbcTemplate, BytesSqlIdHelper idHelper, BytesResultSetMapper mapper, Clock clock) {
        super(jdbcTemplate, idHelper, mapper);
        this.localMapper = mapper;
        this.clock = clock;
    }

    @Override
    public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) {
        String selectSql = """
            SELECT id, status, dlq_status, event_type, payload_type, payload,
                   retry_count, next_retry_at, created_at, updated_at, moved_at
            FROM outbox_dlq_events
            WHERE id IN (
                SELECT id
                FROM outbox_dlq_events
                WHERE dlq_status = ?
                ORDER BY moved_at
                FETCH FIRST ? ROWS ONLY
            )
            FOR UPDATE SKIP LOCKED
        """;
        List<OutboxDlqEvent> events = jdbcTemplate.query(
                selectSql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, batchSize);
                },
                (rs, rowNum) -> mapper.toDlqEvent(rs)
        );
        Set<UUID> ids = events.stream()
                .map(OutboxEvent::getId)
                .collect(Collectors.toSet());
        if (!RepositoryUtils.isIdsValid(ids)) {
            return Collections.emptyList();
        }
        String lockSql = """
            UPDATE outbox_dlq_events
                SET dlq_status = ?, updated_at = ?
            WHERE id IN (%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        jdbcTemplate.update(
                lockSql,
                ps -> {
                    ps.setString(1, lockStatus.name());
                    ps.setTimestamp(2, Timestamp.from(clock.instant()));
                    idHelper.setIdsToPs(ps, 3, ids);
                }
        );
        events.forEach(event -> event.setDlqStatus(lockStatus));
        return events;
    }

    @Override
    public int deleteBatchByStatusAndThreshold(DlqStatus status, Instant threshold, int batchSize) {
        String selectSql = """
            SELECT id FROM outbox_dlq_events
            WHERE dlq_status = ? AND updated_at <= ?
            ORDER BY updated_at
            FETCH FIRST ? ROWS ONLY
        """;
        Set<UUID> ids = new HashSet<>(jdbcTemplate.query(
                selectSql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setTimestamp(2, Timestamp.from(threshold));
                    ps.setInt(3, batchSize);
                },
                (rs, numRow) -> localMapper.fromBytesToUuid(rs.getBytes("id"))
        ));
        if (!RepositoryUtils.isIdsValid(ids)) {
            return 0;
        }
        String sql = """
            DELETE FROM outbox_dlq_events
            WHERE id IN (%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        return jdbcTemplate.update(sql, ps -> idHelper.setIdsToPs(ps, 1, ids));
    }
}
