package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.util.UUID;

public class PostgreSqlOutboxDlqApiRepository extends AbstractOutboxDlqApiRepository {

    public PostgreSqlOutboxDlqApiRepository(JdbcTemplate jdbcTemplate, SqlIdHelper idHelper, ResultSetMapper mapper,
                                            Clock clock) {
        super(jdbcTemplate, idHelper, mapper, clock);
    }

    @Override
    protected Object convertIdParameter(UUID id) {
        return id;
    }
}
