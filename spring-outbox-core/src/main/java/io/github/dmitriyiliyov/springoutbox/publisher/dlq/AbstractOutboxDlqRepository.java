package io.github.dmitriyiliyov.springoutbox.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.utils.ResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.utils.RepositoryUtils;
import io.github.dmitriyiliyov.springoutbox.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractOutboxDlqRepository implements OutboxDlqRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final SqlIdHelper idHelper;
    protected final ResultSetMapper mapper;

    public AbstractOutboxDlqRepository(JdbcTemplate jdbcTemplate, SqlIdHelper idHelper, ResultSetMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.idHelper = idHelper;
        this.mapper = mapper;
    }

    @Transactional
    @Override
    public void saveBatch(List<OutboxDlqEvent> eventBatch) {
        String sql = """
            INSERT INTO outbox_dlq_events
            (id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.batchUpdate(
                sql,
                eventBatch,
                eventBatch.size(),
                (ps, event) -> {
                    idHelper.setIdToPs(ps, 1, event.getId());
                    ps.setString(2, event.getStatus().name());
                    ps.setString(3, event.getDlqStatus().name());
                    ps.setString(4, event.getEventType());
                    ps.setString(5, event.getPayloadType());
                    ps.setString(6, event.getPayload());
                    ps.setInt(7, event.getRetryCount());
                    ps.setTimestamp(8, Timestamp.from(event.getNextRetryAt()));
                    ps.setTimestamp(9, Timestamp.from(event.getCreatedAt()));
                    ps.setTimestamp(10, Timestamp.from(event.getUpdatedAt()));
                    ps.setTimestamp(11, Timestamp.from(event.getMovedAt()));
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
            SELECT id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at
            FROM outbox_dlq_events
            WHERE id = ?
        """;
        List<OutboxDlqEvent> results = jdbcTemplate.query(
                sql,
                ps -> idHelper.setIdToPs(ps, 1, id),
                (rs, rowNum) -> mapper.toDlqEvent(rs)
        );
        return results.stream().findFirst();
    }

    @Transactional
    @Override
    public List<OutboxDlqEvent> findBatch(Set<UUID> ids) {
        if (!RepositoryUtils.isIdsValid(ids)) return List.of();
        String sql = """
            SELECT id, status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at
            FROM outbox_dlq_events
            WHERE id IN (%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        return jdbcTemplate.query(
                sql,
                ps -> {
                    int i = 1;
                    for (UUID id: ids) {
                        idHelper.setIdToPs(ps, i++, id);
                    }
                },
                (rs, rowNum) -> mapper.toDlqEvent(rs)
        );
    }

    @Transactional(readOnly = true)
    @Override
    public List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchNumber, int batchSize) {
        String sql = """
            SELECT id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at
            FROM outbox_dlq_events
            WHERE dlq_status = ?
            ORDER BY moved_at
            LIMIT ? OFFSET ?
        """;
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, batchSize);
                    ps.setInt(3, (batchNumber - 1) * batchSize);
                },
                (rs, rowNum) -> mapper.toDlqEvent(rs)
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
                    idHelper.setIdToPs(ps, 2, id);
                }
        );
    }

    @Transactional
    @Override
    public void updateBatchStatus(Set<UUID> ids, DlqStatus status) {
        if (!RepositoryUtils.isIdsValid(ids)) return;
        String sql = "UPDATE outbox_dlq_events SET status = ? WHERE id IN (" + RepositoryUtils.generateIdsPlaceholders(ids) + ")";
        jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    idHelper.setIdsToPs(ps, 2, ids);
                }
        );
    }

    @Transactional
    @Override
    public void deleteById(UUID id) {
        String sql = "DELETE FROM outbox_dlq_events WHERE id = ?";
        jdbcTemplate.update(sql, ps -> idHelper.setIdToPs(ps, 1, id));
    }

    @Transactional
    @Override
    public void deleteBatch(Set<UUID> ids) {
        if (!RepositoryUtils.isIdsValid(ids)) return;
        String sql = "DELETE FROM outbox_dlq_events WHERE id IN (" + RepositoryUtils.generateIdsPlaceholders(ids) + ")";
        jdbcTemplate.update(sql, ps -> idHelper.setIdsToPs(ps, 1, ids));
    }
}
