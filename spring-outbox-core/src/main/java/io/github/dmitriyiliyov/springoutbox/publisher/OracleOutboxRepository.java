package io.github.dmitriyiliyov.springoutbox.publisher;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.BytesSqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.RepositoryUtils;
import io.github.dmitriyiliyov.springoutbox.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
        if (!RepositoryUtils.validateIds(ids)) {
            return Collections.emptyList();
        }
        String lockSql = """
            UPDATE outbox_events
                SET status = ?, updated_at = ?
            WHERE id IN(%s)
        """.formatted(RepositoryUtils.generatePlaceholders(ids));
        List<Object> params = new ArrayList<>();
        params.add(lockStatus.name());
        Instant updatedAt = Instant.now();
        params.add(Timestamp.from(updatedAt));
        params.addAll(idHelper.convertIdsToDbFormat(ids));
        jdbcTemplate.update(lockSql, params.toArray());
        events.forEach(event -> {
            event.setStatus(lockStatus);
            event.setUpdatedAt(updatedAt);
        });
        return events;
    }

    @Transactional
    @Override
    public int updateBatchStatusByStatus(EventStatus status, int batchSize, EventStatus newStatus) {
        String selectSql = """
            SELECT id
            FROM outbox_events
            WHERE status = ?
            ORDER BY updated_at
            FETCH FIRST ? ROWS ONLY
            FOR UPDATE SKIP LOCKED
        """;
        Set<UUID> ids = new HashSet<>(jdbcTemplate.query(
                selectSql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, batchSize);
                },
                (rs, rowNum) -> mapper.fromBytesToUuid(rs.getBytes("id")))
        );
        if (!RepositoryUtils.validateIds(ids)) {
            return 0;
        }
        String lockSql = """
            UPDATE outbox_events
                SET status = ?, updated_at = ?
            WHERE id IN(%s)
        """.formatted(RepositoryUtils.generatePlaceholders(ids));
        List<Object> params = new ArrayList<>();
        params.add(newStatus.name());
        params.add(Timestamp.from(Instant.now()));
        params.addAll(idHelper.convertIdsToDbFormat(ids));
        return jdbcTemplate.update(lockSql, params.toArray());
    }

    @Transactional
    @Override
    public void deleteBatchByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize) {
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
        if (!RepositoryUtils.validateIds(ids)) {
            return;
        }
        String deleteSql = """
            DELETE FROM outbox_events
            WHERE id IN (%s)
        """.formatted(RepositoryUtils.generatePlaceholders(ids));
        jdbcTemplate.update(
                deleteSql,
                idHelper.convertIdsToDbFormat(ids).toArray()
        );
    }
}
