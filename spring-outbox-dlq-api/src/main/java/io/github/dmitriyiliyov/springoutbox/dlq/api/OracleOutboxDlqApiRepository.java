package io.github.dmitriyiliyov.springoutbox.dlq.api;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class OracleOutboxDlqApiRepository extends MultiDialectOutboxDlqApiRepository {

    public OracleOutboxDlqApiRepository(JdbcTemplate jdbcTemplate, SqlIdHelper idHelper, ResultSetMapper mapper) {
        super(jdbcTemplate, idHelper, mapper);
    }

    @Override
    public List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchNumber, int batchSize) {
        String selectSql = """
            SELECT *
            FROM outbox_dlq_events
            WHERE dlq_status = ?
            ORDER BY moved_at, id
            OFFSET ? ROWS
            FETCH NEXT ? ROWS ONLY
        """;
        return jdbcTemplate.query(
                selectSql,
                ps -> {
                    ps.setString(1, status.name());
                    ps.setInt(2, batchNumber * batchSize);
                    ps.setInt(3, batchSize);
                },
                (rs, rowNum) -> mapper.toDlqEvent(rs)
        );
    }
}
