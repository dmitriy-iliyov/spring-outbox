package io.github.dmitriyiliyov.springoutbox.dlq.api;

import io.github.dmitriyiliyov.springoutbox.core.utils.BytesResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.BytesSqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.util.UUID;

public class MySqlOutboxDlqApiRepository extends AbstractOutboxDlqApiRepository {

    private final BytesSqlIdHelper localIdHelper;

    public MySqlOutboxDlqApiRepository(JdbcTemplate jdbcTemplate, BytesSqlIdHelper idHelper, BytesResultSetMapper mapper,
                                       Clock clock) {
        super(jdbcTemplate, idHelper, mapper, clock);
        this.localIdHelper = idHelper;
    }

    @Override
    protected Object convertIdParameter(UUID id) {
        return localIdHelper.uuidToBytes(id);
    }
}
