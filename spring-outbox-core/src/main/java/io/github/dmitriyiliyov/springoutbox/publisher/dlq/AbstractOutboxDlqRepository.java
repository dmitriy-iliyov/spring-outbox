package io.github.dmitriyiliyov.springoutbox.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.publisher.utils.RepositoryUtils;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.ResultSetMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public abstract class AbstractOutboxDlqRepository implements OutboxDlqRepository {

    protected final JdbcTemplate jdbcTemplate;

    public AbstractOutboxDlqRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    @Override
    public void saveBatch(List<OutboxDlqEvent> eventBatch) {
        String sql = """
            INSERT INTO outbox_dlq_events
            (id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.batchUpdate(
                sql,
                eventBatch,
                eventBatch.size(),
                (ps, event) -> {
                    setId(ps, 1, event.getId());
                    ps.setString(2, event.getStatus().name());
                    ps.setString(3, event.getDlqStatus().name());
                    ps.setString(4, event.getEventType());
                    ps.setString(5, event.getPayloadType());
                    ps.setString(6, event.getPayload());
                    ps.setInt(7, event.getRetryCount());
                    ps.setTimestamp(8, Timestamp.from(event.getNextRetryAt()));
                    ps.setTimestamp(9, Timestamp.from(event.getCreatedAt()));
                    ps.setTimestamp(10, Timestamp.from(event.getUpdatedAt()));
                });
    }

    @Transactional(readOnly = true)
    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM outbox_dlq_events";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    @Transactional(readOnly = true)
    @Override
    public long countByStatus(DlqStatus status) {
        String sql = "SELECT COUNT(*) FROM outbox_dlq_events WHERE dlq_status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status.name());
        return count == null ? 0 : count;
    }

    @Transactional(readOnly = true)
    @Override
    public long countByEventTypeAndStatus(String eventType, DlqStatus status) {
        String sql = "SELECT COUNT(*) FROM outbox_dlq_events WHERE event_type = ? AND dlq_status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, eventType, status.name());
        return count == null ? 0 : count;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<OutboxDlqEvent> findById(UUID id) {
        String sql = """
            SELECT id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at
            FROM outbox_dlq_events
            WHERE id = ?
        """;
        List<OutboxDlqEvent> results = jdbcTemplate.query(
                sql,
                ps -> setId(ps, 1, id),
                (rs, rowNum) -> ResultSetMapper.toDlqEvent(rs)
        );
        return results.stream().findFirst();
    }

    @Transactional
    @Override
    public List<OutboxDlqEvent> findBatch(Set<UUID> ids) {
        if (!RepositoryUtils.validateIds(ids)) return List.of();
        String sql = """
            SELECT id, status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at
            FROM outbox_dlq_events
            WHERE id IN (%s)
        """.formatted(RepositoryUtils.generatePlaceholders(ids));
        return jdbcTemplate.query(
                sql,
                ps -> {
                    int i = 1;
                    for (UUID id: ids) {
                        setId(ps, i++, id);
                    }
                },
                (rs, rowNum) -> ResultSetMapper.toDlqEvent(rs)
        );
    }

    @Transactional(readOnly = true)
    @Override
    public List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchNumber, int batchSize) {
        String sql = """
            SELECT id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at
            FROM outbox_dlq_events
            WHERE dlq_status = ?
            OFFSET ?
            LIMIT ?
        """;
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, (batchNumber - 1) * batchSize);
                    ps.setInt(3, batchSize);
                },
                (rs, rowNum) -> ResultSetMapper.toDlqEvent(rs)
        );
    }

    @Transactional
    @Override
    public void updateStatus(UUID id, DlqStatus status) {
        String sql = "UPDATE outbox_dlq_events SET dlq_status = ? WHERE id = ?";
        jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    setId(ps, 2, id);
                }
        );
    }

    @Transactional
    @Override
    public void updateBatchStatus(Set<UUID> ids, DlqStatus status) {
        if (!RepositoryUtils.validateIds(ids)) return;
        String sql = "UPDATE outbox_dlq_events SET status = ? WHERE id IN (" + RepositoryUtils.generatePlaceholders(ids) + ")";
        List<Object> params = new ArrayList<>();
        params.add(status.name());
        params.addAll(ids);
        jdbcTemplate.update(sql, params.toArray());
    }

    @Transactional
    @Override
    public void deleteById(UUID id) {
        String sql = "DELETE FROM outbox_dlq_events WHERE id = ?";
        jdbcTemplate.update(sql, ps -> setId(ps, 1, id));
    }

    @Transactional
    @Override
    public void deleteBatch(Set<UUID> ids) {
        if (!RepositoryUtils.validateIds(ids)) return;
        String sql = "DELETE FROM outbox_dlq_events WHERE id IN (" + RepositoryUtils.generatePlaceholders(ids) + ")";
        List<Object> params = new ArrayList<>();
        params.addAll(ids);
        jdbcTemplate.update(sql, params.toArray());
    }

    protected abstract void setId(PreparedStatement ps, int parameterIndex, UUID id) throws SQLException;
}
