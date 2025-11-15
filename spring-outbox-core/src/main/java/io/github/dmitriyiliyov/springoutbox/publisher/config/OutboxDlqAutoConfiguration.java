package io.github.dmitriyiliyov.springoutbox.publisher.config;

import io.github.dmitriyiliyov.springoutbox.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.*;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.web.DlqStatusQueryConverter;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.web.OutboxDlqController;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.web.OutboxDlqControllerAdvice;
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
    public OutboxCache<DlqStatus> outboxDlqCache() {
        return new SimpleOutboxCache<>(30, 30, 30);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqManager outboxDlqManager(OutboxDlqRepository repository, OutboxCache<DlqStatus> cache) {
        return new DefaultOutboxDlqManager(repository, cache);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqHandler outboxDlqHandler() {
        return new LogOutboxDlqHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqTransfer outboxDlqTransfer(OutboxManager manager, OutboxDlqManager dlqManager, OutboxDlqHandler handler,
                                               TransactionTemplate transactionTemplate) {
        return new DefaultOutboxDlqTransfer(transactionTemplate, manager, dlqManager, handler);
    }

    @Bean
    public OutboxScheduler outboxDlqScheduler(ScheduledExecutorService outboxScheduledExecutorService,
                                              OutboxPublisherProperties properties, OutboxDlqTransfer transfer) {
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
    public OutboxMetrics outboxDlqMetrics(OutboxPublisherProperties properties, MeterRegistry registry, OutboxDlqManager manager) {
        return new OutboxDlqMetrics(registry, properties, manager);
    }
}
