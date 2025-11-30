package io.github.dmitriyiliyov.springoutbox.publisher;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.utils.BytesSqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.utils.RepositoryUtils;
import io.github.dmitriyiliyov.springoutbox.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

// Oracle 23+
public class OracleOutboxRepository extends AbstractOutboxRepository {

    protected final BytesSqlResultSetMapper mapper;

    public OracleOutboxRepository(JdbcTemplate jdbcTemplate, SqlIdHelper idHelper, BytesSqlResultSetMapper mapper) {
        super(jdbcTemplate, idHelper);
        this.mapper = mapper;
    }

    @Transactional
    @Override
    public List<OutboxEvent> findAndLockBatchByEventTypeAndStatus(String eventType, EventStatus status, int batchSize,
                                                                  EventStatus lockStatus) {
        String selectSql = """
            SELECT *
            FROM outbox_events
            WHERE event_type = ? AND status = ? AND next_retry_at <= ?
            ORDER BY next_retry_at
            FETCH FIRST ? ROWS ONLY
            FOR UPDATE SKIP LOCKED
        """;
        List<OutboxEvent> events = jdbcTemplate.query(
                selectSql,
                ps -> {
                    ps.setString(1, eventType);
                    ps.setString(2, status.name());
                    ps.setTimestamp(3, Timestamp.from(Instant.now()));
                    ps.setInt(4, batchSize);
                },
                (rs, rowNum) -> mapper.toEvent(rs)
        );
       return updateStatus(events, lockStatus);
    }

    @Transactional
    @Override
    public List<OutboxEvent> findAndLockBatchByStatus(EventStatus status, int batchSize, EventStatus lockStatus) {
        String selectSql = """
            SELECT *
            FROM outbox_events
            WHERE status = ?
            ORDER BY updated_at
            FETCH FIRST ? ROWS ONLY
            FOR UPDATE SKIP LOCKED
        """;
        List<OutboxEvent> events = jdbcTemplate.query(
                selectSql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, batchSize);
                },
                (rs, rowNum) -> mapper.toEvent(rs)
        );
        return updateStatus(events, lockStatus);
    }

    private List<OutboxEvent> updateStatus(List<OutboxEvent> events, EventStatus lockStatus) {
        Set<UUID> ids = events.stream()
                .map(OutboxEvent::getId)
                .collect(Collectors.toSet());
        if (!RepositoryUtils.isIdsValid(ids)) {
            return Collections.emptyList();
        }
        String lockSql = """
            UPDATE outbox_events
                SET status = ?, updated_at = ?
            WHERE id IN(%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        Instant updatedAt = Instant.now();
        jdbcTemplate.update(
                lockSql,
                ps -> {
                    ps.setString(1, lockStatus.name());
                    ps.setTimestamp(2, Timestamp.from(updatedAt));
                    idHelper.setIdsToPs(ps, 3, ids);
                }
        );
        events.forEach(event -> {
            event.setStatus(lockStatus);
            event.setUpdatedAt(updatedAt);
        });
        return events;
    }

    @Transactional
    @Override
    public int updateBatchStatusByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize, EventStatus newStatus) {
        String selectSql = """
            SELECT id
            FROM outbox_events
            WHERE status = ? AND updated_at <= ?
            ORDER BY updated_at
            FETCH FIRST ? ROWS ONLY
            FOR UPDATE SKIP LOCKED
        """;
        Set<UUID> ids = new HashSet<>(jdbcTemplate.query(
                selectSql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setTimestamp(2, Timestamp.from(threshold));
                    ps.setInt(3, batchSize);
                },
                (rs, rowNum) -> mapper.fromBytesToUuid(rs.getBytes("id")))
        );
        if (!RepositoryUtils.isIdsValid(ids)) {
            return 0;
        }
        String lockSql = """
            UPDATE outbox_events
                SET status = ?, updated_at = ?
            WHERE id IN(%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        return jdbcTemplate.update(
                lockSql,
                ps -> {
                    ps.setString(1, newStatus.name());
                    ps.setTimestamp(2, Timestamp.from(Instant.now()));
                    idHelper.setIdsToPs(ps, 3, ids);
                }
        );
    }

    @Transactional
    @Override
    public int deleteBatchByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize) {
        String selectSql = """
            SELECT id
            FROM outbox_events
            WHERE status = ? AND updated_at <= ?
            ORDER BY updated_at
            FETCH FIRST ? ROWS ONLY
            FOR UPDATE SKIP LOCKED
        """;
        Set<UUID> ids = new HashSet<>(jdbcTemplate.query(
                selectSql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setTimestamp(2, Timestamp.from(threshold));
                    ps.setInt(3, batchSize);
                },
                (rs, rowNum) -> mapper.fromBytesToUuid(rs.getBytes("id")))
        );
        if (!RepositoryUtils.isIdsValid(ids)) {
            return 0;
        }
        String deleteSql = """
            DELETE FROM outbox_events
            WHERE id IN (%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        return jdbcTemplate.update(
                deleteSql,
                ps -> idHelper.setIdsToPs(ps, 1, ids)
        );
    }
}
