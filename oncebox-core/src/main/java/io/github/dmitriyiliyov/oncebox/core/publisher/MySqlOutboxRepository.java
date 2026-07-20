package io.github.dmitriyiliyov.oncebox.core.publisher;

import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.RepositoryUtils;
import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class MySqlOutboxRepository extends AbstractOutboxRepository {

    protected final ResultSetMapper mapper;

    public MySqlOutboxRepository(JdbcTemplate jdbcTemplate,
                                 Clock clock,
                                 SqlIdHelper idHelper,
                                 BytesResultSetMapper mapper) {
        super(jdbcTemplate, clock, idHelper);
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
    }

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
                    ps.setTimestamp(3, Timestamp.from(clock.instant()));
                    ps.setInt(4, batchSize);
                },
                (rs, rowNum) -> mapper.toEvent(rs)
        );
        return lock(events, lockStatus);
    }

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
        if (!RepositoryUtils.isIdsValid(ids)) {
            return Collections.emptyList();
        }
        String lockSql = """
            UPDATE outbox_events
                SET status = ?, updated_at = ?
            WHERE id IN(%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        Instant updatedAt = clock.instant();
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
                    ps.setTimestamp(2, Timestamp.from(clock.instant()));
                    ps.setString(3, status.name());
                    ps.setTimestamp(4, Timestamp.from(threshold));
                    ps.setInt(5, batchSize);
                }
        );
    }

    @Override
    public int deleteBatchByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize) {
        String sql = """
            DELETE FROM outbox_events
            WHERE status = ? AND updated_at <= ?
            LIMIT ?
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setTimestamp(2, Timestamp.from(threshold));
                    ps.setInt(3, batchSize);
                }
        );
    }
}