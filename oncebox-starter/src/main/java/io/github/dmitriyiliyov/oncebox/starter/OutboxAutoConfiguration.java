package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.OutboxScheduler;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.starter.consumer.OutboxConsumerProperties;
import io.github.dmitriyiliyov.oncebox.starter.publisher.OutboxPublisherProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
@Import(OutboxMetricsAutoConfiguration.class)
public class OutboxAutoConfiguration {

    private final OutboxProperties properties;

    public OutboxAutoConfiguration(OutboxProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "oncebox.publisher",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public OutboxPublisherProperties outboxPublisherProperties() {
        return properties.getPublisher();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "oncebox.consumer",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxConsumerProperties outboxConsumerProperties() {
        return properties.getConsumer();
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxJdbcTemplate")
    public JdbcTemplate outboxJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnDatabaseType(type = DatabaseType.POSTGRESQL)
    public OutboxRepositoryFactory postgreSqlOutboxRepositoryFactory(@Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
                                                                     Clock clock) {
        return new PostgreSqlOutboxRepositoryFactory(jdbcTemplate, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnDatabaseType(type = DatabaseType.MYSQL)
    public OutboxRepositoryFactory mySqlOutboxRepositoryFactory(@Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
                                                                Clock clock) {
        return new MySqlOutboxRepositoryFactory(jdbcTemplate, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnDatabaseType(type = DatabaseType.ORACLE)
    public OutboxRepositoryFactory oracleOutboxRepositoryFactory(@Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
                                                                 Clock clock) {
        return new OracleOutboxRepositoryFactory(jdbcTemplate, clock);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "oncebox.tables",
            name = "auto-create",
            havingValue = "true",
            matchIfMissing = true
    )
    public DataSourceInitializer outboxDataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDataSource(dataSource);
        dataSourceInitializer.setDatabasePopulator(OutboxDatabasePopulatorFactory.create(properties, dataSource));
        return dataSourceInitializer;
    }

    @Bean
    @ConditionalOnAnyCleanUpEnabled
    @ConditionalOnMissingBean
    public DistributedLockRepository outboxDistributedLockRepository(OutboxRepositoryFactory repositoryFactory) {
        return repositoryFactory.createDistributedLockRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxScheduleStrategyListenerSupplier outboxScheduleStrategyListenerSupplier() {
        return new NoopOutboxScheduleStrategyListenerSupplier();
    }

    @Bean
    @ConditionalOnMissingBean
    public ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier() {
        return new NoopContinuableTaskDecoratorSupplier();
    }

    @Bean
    public ScheduledExecutorService outboxScheduledExecutorService() {
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("outbox-thrd-");
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                properties.getThreadPoolSize(),
                threadFactory
        );
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorServiceShutdownHook outboxScheduledExecutorServiceShutdownHook(
            @Qualifier("outboxScheduledExecutorService") ScheduledExecutorService service
    ) {
        return new DefaultScheduledExecutorServiceShutdownHook(service);
    }

    @Bean
    public ApplicationRunner outboxJobsInitializer(List<OutboxJobCreateCommand> commands) {
        return args -> {
            for (OutboxJobCreateCommand command : commands) {
                command.create();
            }
        };
    }

    @Bean
    public PostApplicationReadyOutboxInitializer schedulersOutboxInitializer(Map<String, OutboxScheduler> schedulers) {
        return new SchedulersPostApplicationReadyOutboxInitializer(schedulers);
    }

    @Bean
    public PostApplicationReadyOutboxInitializer compositeOutboxInitializer(
            OutboxProperties properties,
            List<PostApplicationReadyOutboxInitializer> initializers
    ) {
        return new CompositePostApplicationReadyOutboxInitializer(properties, initializers);
    }
}
 