package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.consumer.MySqlConsumedOutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.MySqlDistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.MySqlOutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.MySqlOutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.core.utils.DefaultBytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.MySqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

public class MySqlOutboxRepositoryFactory implements OutboxRepositoryFactory {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public MySqlOutboxRepositoryFactory(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public OutboxRepository createOutboxRepository() {
        return new MySqlOutboxRepository(
                jdbcTemplate,
                clock,
                new MySqlIdHelper(),
                new DefaultBytesResultSetMapper()
        );
    }

    @Override
    public OutboxDlqRepository createOutboxDlqRepository() {
        return new MySqlOutboxDlqRepository(
                jdbcTemplate,
                new MySqlIdHelper(),
                new DefaultBytesResultSetMapper(),
                clock
        );
    }

    @Override
    public DistributedLockRepository createDistributedLockRepository() {
        return new MySqlDistributedLockRepository(jdbcTemplate, new MySqlIdHelper());
    }

    @Override
    public ConsumedOutboxRepository createConsumedOutboxRepository() {
        return new MySqlConsumedOutboxRepository(
                jdbcTemplate,
                clock,
                new MySqlIdHelper(),
                new DefaultBytesResultSetMapper()
        );
    }
}
