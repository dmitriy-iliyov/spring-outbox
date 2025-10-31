package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.utils.RepositoryUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 *  Abstract Multi SQL dialect implementation of {@link OutboxRepository}.
 *  <ul>
 *
 *     <li> uses {@link Transactional @Transactional} with
 *     {@link Propagation#MANDATORY}, ensuring that all outbox entries are persisted atomically
 *     as part of the business transaction.</li>
 *
 *     <li> provides a mechanism for incrementing retry counters and marking permanently failed events.</li>
 *
 *  </ul>
 */
public abstract class AbstractOutboxRepository implements OutboxRepository {

    protected final JdbcTemplate jdbcTemplate;

    public AbstractOutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void save(OutboxEvent event) {
        String sql = """
            INSERT INTO outbox_events 
            (id, status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                event.getId(),
                event.getStatus().name(),
                event.getEventType(),
                event.getPayloadType(),
                event.getPayload(),
                event.getRetryCount(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void saveBatch(List<OutboxEvent> eventBatch) {
        String sql = """
            INSERT INTO outbox_events 
            (id, status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        List<Object[]> params = eventBatch.stream()
                .map(e ->
                        new Object[]{
                                e.getId(),
                                e.getStatus().name(),
                                e.getEventType(),
                                e.getPayloadType(),
                                e.getPayload(),
                                e.getRetryCount(),
                                e.getCreatedAt(),
                                e.getUpdatedAt()
                })
                .toList();
        jdbcTemplate.batchUpdate(sql, params);
    }

    @Transactional(readOnly = true)
    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM outbox_events";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    @Transactional(readOnly = true)
    @Override
    public long countByStatus(EventStatus status) {
        String sql = "SELECT COUNT(*) FROM outbox_events WHERE status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status.name());
        return count == null ? 0 : count;
    }

    @Transactional(readOnly = true)
    @Override
    public long countByEventTypeAndStatus(String eventType, EventStatus status) {
        String sql = "SELECT COUNT(*) FROM outbox_events WHERE event_type = ? AND status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, eventType, status.name());
        return count == null ? 0 : count;
    }

    @Transactional
    @Override
    public void updateBatchStatus(Set<UUID> ids, EventStatus newStatus) {
        if (!RepositoryUtils.validateIds(ids)) return;
        if (newStatus.equals(EventStatus.FAILED)) {
            throw new IllegalArgumentException("Use partiallyUpdateBatch() for update FAILED batch");
        }
        String placeholders = RepositoryUtils.generatePlaceholders(ids);
        String sql;
        List<Object> params = new ArrayList<>();
        if (newStatus.equals(EventStatus.PROCESSED)) {
            sql = "UPDATE outbox_events SET status = ?, updated_at = ? WHERE id IN (" + placeholders + ")";
            params.add(newStatus.name());
            params.add(Timestamp.from(Instant.now()));
            params.addAll(ids);
        } else {
            sql = "UPDATE outbox_events SET status = ? WHERE id IN (" + placeholders + ")";
            params.add(newStatus.name());
            params.addAll(ids);
        }
        jdbcTemplate.update(sql, params.toArray());
    }

    @Transactional
    @Override
    public void partiallyUpdateBatch(List<OutboxEvent> events) {
        if (events == null || events.isEmpty()) return;
        String sql = """
            UPDATE outbox_events
            SET
                retry_count = ?,
                status = ?,
                next_retry_at = ?,
                updated_at = ?
            WHERE id = ?
        """;
        Instant updatedAt = Instant.now();
        jdbcTemplate.batchUpdate(
                sql,
                events,
                events.size(),
                (ps, event) -> {
                    ps.setInt(1, event.getRetryCount());
                    ps.setString(2, event.getStatus().name());
                    ps.setTimestamp(3, Timestamp.from(event.getNextRetryAt()));
                    ps.setTimestamp(4, Timestamp.from(updatedAt));
                    ps.setObject(5, event.getId());
                });
    }

    @Transactional
    @Override
    public void deleteBatch(Set<UUID> ids) {
        if (!RepositoryUtils.validateIds(ids)) return;
        String sql = "DELETE FROM outbox_events WHERE id IN (%s)".formatted(RepositoryUtils.generatePlaceholders(ids));
        jdbcTemplate.update(sql, ids.toArray());
    }
}
