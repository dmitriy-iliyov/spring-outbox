package io.github.dmitriyiliyov.springoutbox.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.BytesSqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.utils.RepositoryUtils;
import io.github.dmitriyiliyov.springoutbox.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MySqlOutboxDlqRepository extends AbstractOutboxDlqRepository {

    public MySqlOutboxDlqRepository(JdbcTemplate jdbcTemplate, SqlIdHelper idHelper, BytesSqlResultSetMapper mapper) {
        super(jdbcTemplate, idHelper, mapper);
    }

    @Transactional
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
        if (!RepositoryUtils.isIdsValid(ids)) {
            return Collections.emptyList();
        }
        String lockSql = """
            UPDATE outbox_dlq_events 
                SET status = ?
            WHERE id IN (%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        jdbcTemplate.update(
                lockSql,
                ps -> {
                    ps.setString(1, lockStatus.name());
                    idHelper.setIdsToPs(ps, 2, ids);
                }
        );
        events.forEach(event -> event.setDlqStatus(lockStatus));
        return events;
    }
}
