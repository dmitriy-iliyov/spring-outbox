package io.github.dmitriyiliyov.oncebox.oracle;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesSqlIdHelper;
import io.github.dmitriyiliyov.oncebox.dlq.api.AbstractOutboxDlqApiRepository;
import io.github.dmitriyiliyov.oncebox.dlq.api.DlqFilter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class OracleOutboxDlqApiRepository extends AbstractOutboxDlqApiRepository {

    private final BytesSqlIdHelper localIdHelper;

    public OracleOutboxDlqApiRepository(JdbcTemplate jdbcTemplate, BytesSqlIdHelper idHelper, BytesResultSetMapper mapper,
                                        Clock clock) {
        super(jdbcTemplate, idHelper, mapper, clock);
        this.localIdHelper = Objects.requireNonNull(idHelper, "idHelper cannot be null");
    }

    @Override
    public List<OutboxDlqEvent> findBatch(DlqFilter filter, int batchNumber, int batchSize) {
        StringBuilder sql = new StringBuilder("""
            SELECT id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at 
            FROM outbox_dlq_events
        """);
        String sqlEnd = """
            ORDER BY moved_at, id 
            OFFSET ? ROWS 
            FETCH NEXT ? ROWS ONLY
        """;
        List<Object> params = new LinkedList<>();

        buildQuery(filter, sql, params);
        sql.append(sqlEnd);
        params.add(batchNumber * batchSize);
        params.add(batchSize);

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> mapper.toDlqEvent(rs),
                params.toArray(new Object[0])
        );
    }

    @Override
    protected Object convertIdParameter(UUID id) {
        return localIdHelper.uuidToBytes(id);
    }
}
