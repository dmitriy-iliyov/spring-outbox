package io.github.dmitriyiliyov.springoutbox.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.RepositoryUtils;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.ResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.stream.Collectors;

public class MySqlOutboxDlqRepository extends PostgreSqlOutboxDlqRepository {

    public MySqlOutboxDlqRepository(JdbcTemplate jdbcTemplate, SqlIdHelper idHelper, ResultSetMapper mapper) {
        super(jdbcTemplate, idHelper, mapper);
    }

    @Override
    public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) {
        String selectSql = """
            SELECT *
            FROM outbox_dlq_events
            WHERE status = ?
            ORDER BY moved_at
            LIMIT ?
            FOR UPDATE SKIP LOCKED
        """;
        List<OutboxDlqEvent> events = jdbcTemplate.query(
                selectSql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, batchSize);
                },
                (rs, rowNum) -> mapper.toDlqEvent(rs)
        );
        Set<UUID> ids = events.stream()
                .map(OutboxEvent::getId)
                .collect(Collectors.toSet());
        if (!RepositoryUtils.validateIds(ids)) {
            return Collections.emptyList();
        }
        String lockSql = "UPDATE outbox_dlq_events SET status = ? WHERE id IN (" + RepositoryUtils.generatePlaceholders(ids) + ")";
        List<Object> params = new ArrayList<>();
        params.add(lockStatus);
        params.addAll(idHelper.convertIdsToDbFormat(ids));
        jdbcTemplate.update(lockSql, params.toArray());
        events.forEach(event -> event.setDlqStatus(lockStatus));
        return events;
    }
}
