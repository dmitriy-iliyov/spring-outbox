package io.github.dmitriyiliyov.springoutbox.publisher;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.RepositoryUtils;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.ResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

// only for MySql 8.0.0+
public class MySqlOutboxRepository extends AbstractOutboxRepository {

    protected final ResultSetMapper mapper;

    public MySqlOutboxRepository(JdbcTemplate jdbcTemplate, SqlIdHelper idHelper, ResultSetMapper mapper) {
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
            LIMIT ?
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
        return lock(events, lockStatus);
    }

    @Transactional
    @Override
    public List<OutboxEvent> findAndLockBatchByStatus(EventStatus status, int batchSize, EventStatus lockStatus) {
        String selectSql = """
            SELECT * 
            FROM outbox_events
            WHERE status = ?
            ORDER BY updated_at
            LIMIT ?
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
        return lock(events, lockStatus);
    }

    private List<OutboxEvent> lock(List<OutboxEvent> events, EventStatus lockStatus) {
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
    public int updateBatchStatusByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize, EventStatus newStatus) {
        String sql = """
            UPDATE outbox_events
                SET status = ?, updated_at = ?
            WHERE id IN (
                SELECT id FROM(
                    SELECT id FROM outbox_events
                    WHERE status = ? AND updated_at <= ?
                    ORDER BY updated_at
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                ) AS to_update
            )
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, newStatus.name());
                    ps.setTimestamp(2, Timestamp.from(Instant.now()));
                    ps.setString(3, status.name());
                    ps.setTimestamp(4, Timestamp.from(threshold));
                    ps.setInt(5, batchSize);
                }
        );
    }

    @Transactional
    @Override
    public void deleteBatchByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize) {
        String sql = """
            DELETE FROM outbox_events 
            WHERE id IN (
                SELECT id FROM(
                    SELECT id FROM outbox_events 
                    WHERE status = ? AND updated_at <= ?
                    ORDER BY updated_at
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                ) AS to_delete 
            )
        """;
        jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setTimestamp(2, Timestamp.from(threshold));
                    ps.setInt(3, batchSize);
                }
        );
    }
}
