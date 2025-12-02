package io.github.dmitriyiliyov.springoutbox.publisher.config;

import io.github.dmitriyiliyov.springoutbox.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.*;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.web.DlqStatusQueryConverter;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.web.OutboxDlqController;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.web.OutboxDlqControllerAdvice;
import io.github.dmitriyiliyov.springoutbox.publisher.metrics.OutboxDlqManagerMetricsDecorator;
import io.github.dmitriyiliyov.springoutbox.publisher.metrics.OutboxDlqMetrics;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.OutboxCache;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.SimpleOutboxCache;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ConditionalOnProperty(prefix = "outbox.publisher.dlq", name = "enabled", havingValue = "true")
public class OutboxDlqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqRepository outboxDlqRepository(DataSource dataSource) {
        return OutboxDlqRepositoryFactory.generate(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "outbox.publish.dlq.metrics.gauge.cache",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public OutboxCache<DlqStatus> outboxDlqCache(OutboxPublisherProperties properties) {
        List<Duration> ttls = properties.getDlq().getMetrics().getGauge().getCache().getTtls();
        if (ttls == null || ttls.isEmpty()) {
            throw new IllegalArgumentException("Dlq cache ttls cannot be null or empty");
        }
        if (ttls.size() != 3) {
            throw new IllegalArgumentException("Ttls should be 3 element size");
        }
        return new SimpleOutboxCache<>(
                ttls.get(0).toSeconds(), ttls.get(1).toSeconds(), ttls.get(2).toSeconds()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqManager outboxDlqManager(OutboxDlqRepository repository,
                                             OutboxCache<DlqStatus> cache,
                                             OutboxPublisherProperties properties,
                                             MeterRegistry registry) {
        return new OutboxDlqManagerMetricsDecorator(
                new DefaultOutboxDlqManager(repository, cache),
                properties,
                registry
        );
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
                                               TransactionTemplate transactionTemplate) {
        return new DefaultOutboxDlqTransfer(transactionTemplate, manager, dlqManager, handler);
    }

    @Bean
    public OutboxScheduler outboxDlqScheduler(ScheduledExecutorService outboxScheduledExecutorService,
                                              OutboxPublisherProperties properties,
                                              OutboxDlqTransfer transfer) {
        OutboxPublisherProperties.DlqProperties dlqProperties = properties.getDlq();
        if (dlqProperties == null) {
            throw new IllegalStateException("OutboxProperties.DlqProperties is null");
        }
        return new OutboxDlqTransferScheduler(dlqProperties, outboxScheduledExecutorService, transfer);
    }

    @Bean
    public OutboxDlqController outboxDlqController(OutboxDlqManager dlqManager) {
        return new OutboxDlqController(dlqManager);
    }

    @Bean
    public DlqStatusQueryConverter dlqStatusQueryConverter() {
        return new DlqStatusQueryConverter();
    }

    @Bean
    public OutboxDlqControllerAdvice outboxDlqControllerAdvice() {
        return new OutboxDlqControllerAdvice();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "outbox.publish.dlq.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetrics outboxDlqMetrics(OutboxPublisherProperties properties,
                                          MeterRegistry registry,
                                          OutboxDlqManager manager) {
        return new OutboxDlqMetrics(registry, properties, manager);
    }
}
