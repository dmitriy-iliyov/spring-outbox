package io.github.dmitriyiliyov.springoutbox.core.it.config;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OracleConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.DefaultOutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OracleOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.*;
import io.github.dmitriyiliyov.springoutbox.core.utils.DefaultBytesSqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.OracleSqlIdHelper;
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

@TestConfiguration
@Profile("oracle-it")
public class OracleIntegrationTestsConfig {

    @Bean
    public DataSourceInitializer oracleOutboxDataSourceInitializer(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSeparator("/");
        populator.setScripts(
                new ClassPathResource("oracle/oracle_outbox_table.sql"),
                new ClassPathResource("oracle/oracle_outbox_dlq_table.sql"),
                new ClassPathResource("oracle/oracle_outbox_consumed_table.sql")
        );
        populator.setContinueOnError(false);
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }

    @Bean
    public OutboxDlqRepository oracleOutboxDlqRepository(DataSource dataSource) {
        return new OracleOutboxDlqRepository(new JdbcTemplate(dataSource), new OracleSqlIdHelper(), new DefaultBytesSqlResultSetMapper());
    }

    @Bean
    public OutboxRepository oracleOutboxRepository(DataSource dataSource) {
        return new OracleOutboxRepository(new JdbcTemplate(dataSource), new OracleSqlIdHelper(), new DefaultBytesSqlResultSetMapper());
    }

    @Bean
    public ConsumedOutboxRepository oracleConsumedOutboxRepository(DataSource dataSource) {
        return new OracleConsumedOutboxRepository(new JdbcTemplate(dataSource), new OracleSqlIdHelper(), new DefaultBytesSqlResultSetMapper());
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public OutboxDlqManager oracleOutboxDlqManager(@Qualifier("oracleOutboxDlqRepository") OutboxDlqRepository repository) {
        return new DefaultOutboxDlqManager(repository);
    }

    @Bean
    public OutboxManager oracleOutboxManager(@Qualifier("oracleOutboxRepository") OutboxRepository repository) {
        return new DefaultOutboxManager(repository);
    }

    @Bean
    public OutboxDlqTransfer oracleOutboxDlqTransfer(
            PlatformTransactionManager transactionManager,
            @Qualifier("oracleOutboxManager") OutboxManager manager,
            @Qualifier("oracleOutboxDlqManager") OutboxDlqManager dlqManager
    ) {
        return new DefaultOutboxDlqTransfer(
                new TransactionTemplate(transactionManager),
                manager,
                dlqManager,
                new LogOutboxDlqHandler()
        );
    }
}
