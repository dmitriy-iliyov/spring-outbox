package io.github.dmitriyiliyov.springoutbox.core.it.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.MySqlConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.e2e.BusinessService;
import io.github.dmitriyiliyov.springoutbox.core.publisher.*;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.*;
import io.github.dmitriyiliyov.springoutbox.core.publisher.utils.UuidV7Generator;
import io.github.dmitriyiliyov.springoutbox.core.utils.DefaultBytesSqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.MySqlIdHelper;
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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static io.github.dmitriyiliyov.springoutbox.core.e2e.BusinessService.EVENT_TYPE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
@Profile("mysql-it")
public class MySqlIntegrationTestsConfig {

    @Bean
    public DataSourceInitializer myslqOutboxDataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDataSource(dataSource);
        dataSourceInitializer.setDatabasePopulator(
                new ResourceDatabasePopulator(
                        false,
                        false,
                        StandardCharsets.UTF_8.name(),
                        new ClassPathResource("mysql/mysql_outbox_table.sql"),
                        new ClassPathResource("mysql/mysql_outbox_dlq_table.sql"),
                        new ClassPathResource("mysql/mysql_outbox_consumed_table.sql"),
                        new ClassPathResource("mysql/mysql_business_table.sql"))
        );
        return dataSourceInitializer;
    }

    @Bean
    public OutboxDlqRepository mysqlOutboxDlqRepository(DataSource dataSource) {
        return new MySqlOutboxDlqRepository(new JdbcTemplate(dataSource), new MySqlIdHelper(), new DefaultBytesSqlResultSetMapper());
    }

    @Bean
    public OutboxRepository mysqlOutboxRepository(DataSource dataSource) {
        return new MySqlOutboxRepository(new JdbcTemplate(dataSource), new MySqlIdHelper(), new DefaultBytesSqlResultSetMapper());
    }

    @Bean
    public ConsumedOutboxRepository mysqlConsumedOutboxRepository(DataSource dataSource) {
        return new MySqlConsumedOutboxRepository(new JdbcTemplate(dataSource), new MySqlIdHelper(), new DefaultBytesSqlResultSetMapper());
    }

    @Bean
    public JdbcTemplate mysqlJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public OutboxDlqManager mysqlOutboxDlqManager(@Qualifier("mysqlOutboxDlqRepository") OutboxDlqRepository repository) {
        return new DefaultOutboxDlqManager(repository);
    }

    @Bean
    public OutboxManager mysqlOutboxManager(@Qualifier("mysqlOutboxRepository") OutboxRepository repository) {
        return new DefaultOutboxManager(repository);
    }

    @Bean
    public OutboxDlqTransfer mysqlOutboxDlqTransfer(
            PlatformTransactionManager transactionManager,
            @Qualifier("mysqlOutboxManager") OutboxManager manager,
            @Qualifier("mysqlOutboxDlqManager") OutboxDlqManager dlqManager
    ) {
        return new DefaultOutboxDlqTransfer(
                new TransactionTemplate(transactionManager),
                manager,
                dlqManager,
                new LogOutboxDlqHandler()
        );
    }

    @Bean
    public OutboxPublisher mysqlOutboxPublisher(@Qualifier("mysqlOutboxManager") OutboxManager manager) {
        OutboxPublisherPropertiesHolder propertiesHolder = mock(OutboxPublisherPropertiesHolder.class);
        when(propertiesHolder.existEventType(EVENT_TYPE)).thenReturn(true);
        return new DefaultOutboxPublisher(
                propertiesHolder,
                new JacksonOutboxSerializer(new ObjectMapper(), new UuidV7Generator()),
                manager
        );
    }

    @Bean
    public BusinessService mysqlBusinessService(
            @Qualifier("mysqlOutboxPublisher") OutboxPublisher publisher,
            @Qualifier("mysqlJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new BusinessService(
                publisher,
                id -> {
                    ByteBuffer bb = ByteBuffer.allocate(16);
                    bb.putLong(id.getMostSignificantBits());
                    bb.putLong(id.getLeastSignificantBits());
                    return bb.array();
                },
                jdbcTemplate
        );
    }
}
