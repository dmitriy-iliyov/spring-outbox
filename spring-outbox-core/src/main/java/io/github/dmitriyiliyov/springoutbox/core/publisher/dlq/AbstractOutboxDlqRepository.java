package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.utils.RepositoryUtils;
import io.github.dmitriyiliyov.springoutbox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractOutboxDlqRepository implements OutboxDlqRepository {

    protected final JdbcTemplate jdbcTemplate;
    protected final SqlIdHelper idHelper;
    protected final ResultSetMapper mapper;

    public AbstractOutboxDlqRepository(JdbcTemplate jdbcTemplate, SqlIdHelper idHelper, ResultSetMapper mapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.idHelper = Objects.requireNonNull(idHelper, "idHelper cannot be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
    }

    @Override
    public void saveBatch(List<OutboxDlqEvent> eventBatch) {
        String sql = """
            INSERT INTO outbox_dlq_events
            (id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                    ps.setTimestamp(10, Timestamp.from(event.getMovedAt()));
                    ps.setTimestamp(11, Timestamp.from(event.getMovedAt()));
                });
    }

    @Override
    public int deleteBatch(Set<UUID> ids) {
        if (!RepositoryUtils.isIdsValid(ids)) return 0;
        String sql = "DELETE FROM outbox_dlq_events WHERE id IN (" + RepositoryUtils.generateIdsPlaceholders(ids) + ")";
        return jdbcTemplate.update(sql, ps -> idHelper.setIdsToPs(ps, 1, ids));
    }
}