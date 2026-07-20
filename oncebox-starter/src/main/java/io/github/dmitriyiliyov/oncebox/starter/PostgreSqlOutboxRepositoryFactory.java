package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.consumer.PostgreSqlConsumedOutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.PostgreSqlDistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.PostgreSqlOutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.PostgreSqlOutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.core.utils.DefaultResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.PostgreSqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

public class PostgreSqlOutboxRepositoryFactory implements OutboxRepositoryFactory {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public PostgreSqlOutboxRepositoryFactory(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public OutboxRepository createOutboxRepository() {
        return new PostgreSqlOutboxRepository(
                jdbcTemplate,
                clock,
                new PostgreSqlIdHelper(),
                new DefaultResultSetMapper()
        );
    }

    @Override
    public OutboxDlqRepository createOutboxDlqRepository() {
        return new PostgreSqlOutboxDlqRepository(
                jdbcTemplate,
                new PostgreSqlIdHelper(),
                new DefaultResultSetMapper(),
                clock
        );
    }

    @Override
    public DistributedLockRepository createDistributedLockRepository() {
        return new PostgreSqlDistributedLockRepository(jdbcTemplate, new PostgreSqlIdHelper());
    }

    @Override
    public ConsumedOutboxRepository createConsumedOutboxRepository() {
        return new PostgreSqlConsumedOutboxRepository(jdbcTemplate, clock);
    }
}
