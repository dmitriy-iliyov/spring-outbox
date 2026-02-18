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
import io.github.dmitriyiliyov.springoutbox.web.DlqStatusQueryConverter;
import io.github.dmitriyiliyov.springoutbox.web.OutboxDlqController;
import io.github.dmitriyiliyov.springoutbox.web.OutboxDlqControllerAdvice;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ConditionalOnProperty(prefix = "outbox.publisher.dlq", name = "enabled", havingValue = "true")
public class OutboxDlqAutoConfiguration {


    private static final Logger log = LoggerFactory.getLogger(OutboxDlqAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqRepository outboxDlqRepository(DataSource dataSource,
                                                   @Qualifier("outboxTransactionAwareJdbcTemplate")
                                                   JdbcTemplate jdbcTemplate) {
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
    public OutboxDlqManager outboxDlqManager(OutboxDlqRepository repository,
                                             OutboxPublisherProperties properties,
                                             MeterRegistry registry) {
        OutboxDlqManager manager = new DefaultOutboxDlqManager(repository);
        if (properties.getDlq().getMetrics() == null || !properties.getDlq().getMetrics().isEnabled()) {
            return manager;
        }
        return new OutboxDlqManagerMetricsDecorator(properties, registry, manager);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqHandler outboxDlqHandler() {
        return new LogOutboxDlqHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqTransfer outboxDlqTransfer(OutboxManager manager,
                                               OutboxDlqManager dlqManager,
                                               OutboxDlqHandler handler,
                                               TransactionTemplate transactionTemplate,
                                               OutboxPublisherProperties properties,
                                               MeterRegistry registry) {
        OutboxDlqTransfer outboxDlqTransfer = new DefaultOutboxDlqTransfer(
                transactionTemplate, manager, dlqManager, handler
        );
        if (properties.getDlq().getMetrics() == null || !properties.getDlq().getMetrics().isEnabled()) {
            return outboxDlqTransfer;
        }
        return new OutboxDlqTransferMetricsDecorator(registry, outboxDlqTransfer);
    }

    @Bean
    public OutboxScheduler outboxDlqScheduler(ScheduledExecutorService executorService,
                                              OutboxPublisherProperties properties,
                                              OutboxDlqTransfer transfer) {
        return new OutboxDlqTransferScheduler(properties.getDlq(), executorService, transfer);
    }

    @Bean
    @ConditionalOnClass(OutboxDlqController.class)
    public OutboxDlqController outboxDlqController(OutboxDlqManager dlqManager) {
        return new OutboxDlqController(dlqManager);
    }

    @Bean
    @ConditionalOnClass(DlqStatusQueryConverter.class)
    public DlqStatusQueryConverter dlqStatusQueryConverter() {
        return new DlqStatusQueryConverter();
    }

    @Bean
    @ConditionalOnClass(OutboxDlqControllerAdvice.class)
    public OutboxDlqControllerAdvice outboxDlqControllerAdvice() {
        return new OutboxDlqControllerAdvice();
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
    public OutboxDlqMetricsRepository outboxDlqMetricsRepository(@Qualifier("outboxTransactionAwareJdbcTemplate")
                                                                 JdbcTemplate jdbcTemplate) {
        return new MultiSqlDialectOutboxDlqMetricsRepository(jdbcTemplate);
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
