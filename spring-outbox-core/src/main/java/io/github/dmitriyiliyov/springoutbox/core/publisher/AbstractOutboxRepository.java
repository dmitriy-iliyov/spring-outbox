package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.utils.RepositoryUtils;
import io.github.dmitriyiliyov.springoutbox.core.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
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
    protected final SqlIdHelper idHelper;

    public AbstractOutboxRepository(JdbcTemplate jdbcTemplate, SqlIdHelper idHelper) {
        this.jdbcTemplate = jdbcTemplate;
        this.idHelper = idHelper;
    }

    @Override
    public void save(OutboxEvent event) {
        String sql = """
            INSERT INTO outbox_events 
            (id, status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                ps -> {
                    idHelper.setIdToPs(ps, 1, event.getId());
                    ps.setString(2, event.getStatus().name());
                    ps.setString(3, event.getEventType());
                    ps.setString(4, event.getPayloadType());
                    ps.setString(5, event.getPayload());
                    ps.setInt(6, event.getRetryCount());
                    ps.setTimestamp(7, Timestamp.from(event.getNextRetryAt()));
                    ps.setTimestamp(8, Timestamp.from(event.getCreatedAt()));
                    ps.setTimestamp(9, Timestamp.from(event.getUpdatedAt()));
                }
        );
    }

    @Override
    public void saveBatch(List<OutboxEvent> eventBatch) {
        String sql = """
            INSERT INTO outbox_events 
            (id, status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.batchUpdate(
                sql,
                eventBatch,
                eventBatch.size(),
                (ps, event) -> {
                    idHelper.setIdToPs(ps, 1, event.getId());
                    ps.setString(2, event.getStatus().name());
                    ps.setString(3, event.getEventType());
                    ps.setString(4, event.getPayloadType());
                    ps.setString(5, event.getPayload());
                    ps.setInt(6, event.getRetryCount());
                    ps.setTimestamp(7, Timestamp.from(event.getNextRetryAt()));
                    ps.setTimestamp(8, Timestamp.from(event.getCreatedAt()));
                    ps.setTimestamp(9, Timestamp.from(event.getUpdatedAt()));
                }
        );
    }

    @Override
    public int updateBatchStatus(Set<UUID> ids, EventStatus newStatus) {
        if (!RepositoryUtils.isIdsValid(ids)) return 0;
        if (newStatus.equals(EventStatus.FAILED)) {
            throw new IllegalArgumentException("Use partiallyUpdateBatch() for update FAILED batch");
        }
        String placeholders = RepositoryUtils.generateIdsPlaceholders(ids);
        String sql;
        if (newStatus.equals(EventStatus.PROCESSED)) {
            sql = "UPDATE outbox_events SET status = ?, updated_at = ? WHERE id IN (" + placeholders + ")";
        } else {
            sql = "UPDATE outbox_events SET status = ?, updated_at = ? WHERE id IN (" + placeholders + ")";
        }
        return jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, newStatus.name());
                    ps.setTimestamp(2, Timestamp.from(Instant.now()));
                    idHelper.setIdsToPs(ps, 3, ids);
                }
        );
    }

    @Override
    public int partiallyUpdateBatch(List<OutboxEvent> events) {
        if (events == null || events.isEmpty()) return 0;
        String sql = """
            UPDATE outbox_events
            SET
                retry_count = ?,
                status = ?,
                next_retry_at = ?,
                updated_at = ?
            WHERE id = ?
        """;
        return jdbcTemplate.batchUpdate(
                sql,
                events,
                events.size(),
                (ps, event) -> {
                    ps.setInt(1, event.getRetryCount());
                    ps.setString(2, event.getStatus().name());
                    ps.setTimestamp(3, Timestamp.from(event.getNextRetryAt()));
                    ps.setTimestamp(4, Timestamp.from(Instant.now()));
                    idHelper.setIdToPs(ps, 5, event.getId());
                }
        ).length;
    }

    @Override
    public int deleteBatch(Set<UUID> ids) {
        if (!RepositoryUtils.isIdsValid(ids)) return 0;
        String sql = "DELETE FROM outbox_events WHERE id IN (%s)".formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        return jdbcTemplate.update(sql, ps -> idHelper.setIdsToPs(ps, 1, ids));
    }
}
