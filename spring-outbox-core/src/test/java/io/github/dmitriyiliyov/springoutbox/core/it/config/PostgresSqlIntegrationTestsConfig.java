package io.github.dmitriyiliyov.springoutbox.core.it.config;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.PostgreSqlConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.DefaultOutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.PostgreSqlOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.*;
import io.github.dmitriyiliyov.springoutbox.core.utils.DefaultResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.PostgreSqlIdHelper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

@TestConfiguration
@Profile("postgres-it")
public class PostgresSqlIntegrationTestsConfig {

    @Bean
    public DataSourceInitializer postgresOutboxDataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDataSource(dataSource);
        dataSourceInitializer.setDatabasePopulator(
                new ResourceDatabasePopulator(
                        false,
                        false,
                        StandardCharsets.UTF_8.name(),
                        new ClassPathResource("psql/psql_outbox_table.sql"),
                        new ClassPathResource("psql/psql_outbox_dlq_table.sql"),
                        new ClassPathResource("psql/psql_outbox_consumed_table.sql"),
                        new ClassPathResource("psql/psql_business_table.sql"))
        );
        return dataSourceInitializer;
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public OutboxDlqRepository postgresOutboxDlqRepository(DataSource dataSource) {
        return new PostgreSqlOutboxDlqRepository(new JdbcTemplate(dataSource), new PostgreSqlIdHelper(), new DefaultResultSetMapper());
    }

    @Bean
    public OutboxRepository postgresOutboxRepository(DataSource dataSource,  Clock clock) {
        return new PostgreSqlOutboxRepository(new JdbcTemplate(dataSource), clock, new PostgreSqlIdHelper(), new DefaultResultSetMapper());
    }

    @Bean
    public ConsumedOutboxRepository postgresConsumedOutboxRepository(DataSource dataSource, Clock clock) {
        return new PostgreSqlConsumedOutboxRepository(new JdbcTemplate(dataSource), clock);
    }

    @Bean
    public JdbcTemplate postgresJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public OutboxDlqManager postgresOutboxDlqManager(@Qualifier("postgresOutboxDlqRepository") OutboxDlqRepository repository) {
        return new DefaultOutboxDlqManager(repository);
    }

    @Bean
    public OutboxManager postgresOutboxManager(@Qualifier("postgresOutboxRepository") OutboxRepository repository, Clock clock) {
        return new DefaultOutboxManager(repository, clock);
    }

    @Bean
    public OutboxDlqTransfer postgresOutboxDlqTransfer(
            PlatformTransactionManager transactionManager,
            @Qualifier("postgresOutboxManager") OutboxManager manager,
            @Qualifier("postgresOutboxDlqManager") OutboxDlqManager dlqManager,
            Clock clock
    ) {
        return new DefaultOutboxDlqTransfer(
                new TransactionTemplate(transactionManager),
                manager,
                dlqManager,
                new DefaultOutboxDlqEventMapper(clock),
                new LogOutboxDlqHandler()
        );
    }
}
