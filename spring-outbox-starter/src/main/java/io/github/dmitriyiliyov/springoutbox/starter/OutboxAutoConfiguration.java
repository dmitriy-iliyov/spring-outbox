package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.starter.consumer.OutboxConsumerProperties;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxAutoConfiguration {

    private final OutboxProperties properties;

    public OutboxAutoConfiguration(OutboxProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "outbox.publisher",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public OutboxPublisherProperties outboxPublisherProperties() {
        return properties.getPublisher();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "outbox.consumer",
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
            prefix = "outbox.tables",
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
    @Primary
    @ConditionalOnAnyMetricsEnabled
    public OutboxScheduleStrategyListenerSupplier metricsOutboxScheduleStrategyListenerSupplier(MeterRegistry registry) {
        return new MetricsOutboxScheduleStrategyListenerSupplier(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier() {
        return new NoopContinuableTaskDecoratorSupplier();
    }

    @Bean
    @Primary
    @ConditionalOnAnyMetricsEnabled
    public ContinuableTaskDecoratorSupplier metricsContinuableTaskDecoratorSupplier(MeterRegistry registry) {
        return new ContinuableTaskTimeMeasureDecoratorSupplier(registry);
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService outboxScheduledExecutorService() {
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("otbx-thread-");
        threadFactory.setDaemon(true);
        threadFactory.setThreadPriority(Thread.NORM_PRIORITY);
        return Executors.newScheduledThreadPool(
                properties.getThreadPoolSize(),
                threadFactory
        );
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
    public PostApplicationReadyOutboxInitializer outboxInitializer(OutboxProperties properties,
                                                                   Map<String, OutboxScheduler> schedulers,
                                                                   Map<String, OutboxMetrics> metrics) {
        return new PostApplicationReadyOutboxInitializer(properties, schedulers, metrics);
    }
}
 