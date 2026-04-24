package io.github.dmitriyiliyov.springoutbox.dlq.api;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.core.utils.RepositoryUtils;
import io.github.dmitriyiliyov.springoutbox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class MultiDialectOutboxDlqApiRepository implements OutboxDlqApiRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final SqlIdHelper idHelper;
    protected final ResultSetMapper mapper;

    public MultiDialectOutboxDlqApiRepository(JdbcTemplate jdbcTemplate, SqlIdHelper idHelper, ResultSetMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.idHelper = idHelper;
        this.mapper = mapper;
    }

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

    @Override
    public List<OutboxDlqEvent> findBatch(Set<UUID> ids) {
        if (!RepositoryUtils.isIdsValid(ids)) return List.of();
        String sql = """
            SELECT id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at
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

    @Override
    public List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchNumber, int batchSize) {
        String sql = """
            SELECT id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at
            FROM outbox_dlq_events
            WHERE dlq_status = ?
            ORDER BY moved_at, id
            LIMIT ? OFFSET ?
        """;
        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, batchSize);
                    ps.setInt(3, batchNumber * batchSize);
                },
                (rs, rowNum) -> mapper.toDlqEvent(rs)
        );
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM outbox_dlq_events";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public long countByStatus(DlqStatus status) {
        String sql = "SELECT COUNT(*) FROM outbox_dlq_events WHERE dlq_status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status.name());
        return count == null ? 0 : count;
    }

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

    @Override
    public void updateBatchStatus(Set<UUID> ids, DlqStatus status) {
        if (!RepositoryUtils.isIdsValid(ids)) return;
        String sql = "UPDATE outbox_dlq_events SET dlq_status = ? WHERE id IN (" + RepositoryUtils.generateIdsPlaceholders(ids) + ")";
        jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, status.name());
                    idHelper.setIdsToPs(ps, 2, ids);
                }
        );
    }

    @Override
    public int deleteById(UUID id) {
        String sql = "DELETE FROM outbox_dlq_events WHERE id = ?";
        return jdbcTemplate.update(sql, ps -> idHelper.setIdToPs(ps, 1, id));
    }

    @Override
    public int deleteBatch(Set<UUID> ids) {
        if (!RepositoryUtils.isIdsValid(ids)) return 0;
        String sql = "DELETE FROM outbox_dlq_events WHERE id IN (" + RepositoryUtils.generateIdsPlaceholders(ids) + ")";
        return jdbcTemplate.update(sql, ps -> idHelper.setIdsToPs(ps, 1, ids));
    }
}
