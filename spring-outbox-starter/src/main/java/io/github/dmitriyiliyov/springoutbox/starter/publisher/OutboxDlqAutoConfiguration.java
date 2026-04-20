package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.*;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq.*;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.OutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.springoutbox.starter.OutboxProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ConditionalOnProperty(prefix = "outbox.publisher.dlq", name = "enabled", havingValue = "true")
public class OutboxDlqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqRepository outboxDlqRepository(
            DataSource dataSource,
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return OutboxDlqRepositoryFactory.generate(dataSource, jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxCache<DlqStatus> outboxDlqCache(OutboxPublisherProperties properties) {
        OutboxProperties.MetricsProperties metricsProperties = properties.getDlq().getMetrics();
        if (metricsProperties != null && metricsProperties.isEnabled()) {
            OutboxProperties.MetricsProperties.GaugeProperties gaugeProperties = metricsProperties.getGauge();
            if (gaugeProperties != null && gaugeProperties.isEnabled()) {
                OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties cacheProperties =
                        gaugeProperties.getCache();
                if (cacheProperties != null && cacheProperties.isEnabled()) {
                    List<Duration> ttls = cacheProperties.getTtls();
                    if (ttls == null || ttls.isEmpty()) {
                        throw new IllegalArgumentException("Cache ttls cannot be null or empty");
                    }
                    if (ttls.size() != 3) {
                        throw new IllegalArgumentException("Ttls should be 3 element size");
                    }
                    return new SimpleOutboxCache<>(
                            ttls.get(0).toSeconds(), ttls.get(1).toSeconds(), ttls.get(2).toSeconds()
                    );
                }
            }
        }
        return new NoopOutboxCache<>();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqManager outboxDlqManager(OutboxDlqRepository repository) {
        return new DefaultOutboxDlqManager(repository);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "outbox.publisher.dlq.metrics",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxDlqManager outboxDlqManagerMetricsDecorator(OutboxDlqManager manager,
                                                             MeterRegistry registry) {
        return new OutboxDlqManagerMetricsDecorator(registry, manager);

    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqHandler outboxDlqHandler() {
        return new LogOutboxDlqHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqEventMapper outboxDlqEventMapper(Clock clock) {
        return new DefaultOutboxDlqEventMapper(clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqTransfer outboxDlqTransfer(TransactionTemplate transactionTemplate,
                                               OutboxManager manager,
                                               OutboxDlqManager dlqManager,
                                               OutboxDlqEventMapper mapper,
                                               OutboxDlqHandler handler) {
        return new DefaultOutboxDlqTransfer(transactionTemplate, manager, dlqManager, mapper, handler);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "outbox.publisher.dlq.metrics",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxDlqTransfer outboxDlqTransferMetricsDecorator(OutboxDlqTransfer transfer,
                                                               MeterRegistry registry) {
        return new OutboxDlqTransferMetricsDecorator(registry, transfer);
    }

    @Bean
    public OutboxScheduler outboxDlqScheduler(ScheduledExecutorService executorService,
                                              OutboxPublisherProperties properties,
                                              OutboxDlqTransfer transfer) {
        return new OutboxDlqTransferScheduler(properties.getDlq(), executorService, transfer);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "outbox.publisher.dlq.metrics",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnProperty(
            prefix = "outbox.publisher.dlq.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxDlqMetricsRepository outboxDlqMetricsRepository(
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new MultiDialectOutboxDlqMetricsRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "outbox.publisher.dlq.metrics",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnProperty(
            prefix = "outbox.publisher.dlq.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxDlqMetricsService outboxDlqMetricsService(OutboxDlqMetricsRepository repository,
                                                           OutboxCache<DlqStatus> cache) {
        return new DefaultOutboxDlqMetricsService(repository, cache);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "outbox.publisher.dlq.metrics",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnProperty(
            prefix = "outbox.publisher.dlq.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetrics outboxDlqMetrics(OutboxPublisherProperties properties,
                                          MeterRegistry registry,
                                          OutboxDlqMetricsService metricsService) {
        return new OutboxDlqMetrics(properties, registry, metricsService);
    }
}
