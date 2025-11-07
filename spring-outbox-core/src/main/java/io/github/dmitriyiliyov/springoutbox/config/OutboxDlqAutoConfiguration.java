package io.github.dmitriyiliyov.springoutbox.config;

import io.github.dmitriyiliyov.springoutbox.core.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.dlq.*;
import io.github.dmitriyiliyov.springoutbox.dlq.api.DlqStatusQueryConverter;
import io.github.dmitriyiliyov.springoutbox.dlq.api.OutboxDlqController;
import io.github.dmitriyiliyov.springoutbox.dlq.api.OutboxDlqControllerAdvice;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxDlqMetrics;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.utils.OutboxCache;
import io.github.dmitriyiliyov.springoutbox.utils.SimpleOutboxCache;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ConditionalOnProperty(prefix = "outbox.dlq", name = "enabled", havingValue = "true")
public class OutboxDlqAutoConfiguration {

    @Bean
    public OutboxDlqRepository outboxDlqRepository(DataSource dataSource) {
        return OutboxDlqRepositoryFactory.generate(dataSource);
    }

    @Bean
    public OutboxCache<DlqStatus> outboxDlqCache() {
        return new SimpleOutboxCache<>(30, 30, 30);
    }

    @Bean
    public OutboxDlqManager outboxDlqManager(OutboxDlqRepository repository, OutboxCache<DlqStatus> cache) {
        return new DefaultOutboxDlqManager(repository, cache);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqHandler outboxDlqHandler() {
        return new LogOutboxDlqHandler();
    }

    @Bean
    public OutboxDlqTransfer outboxDlqTransfer(OutboxManager manager, OutboxDlqManager dlqManager, OutboxDlqHandler handler,
                                               TransactionTemplate transactionTemplate) {
        return new DefaultOutboxDlqTransfer(transactionTemplate, manager, dlqManager, handler);
    }

    @Bean
    public OutboxScheduler outboxDlqScheduler(ScheduledExecutorService outboxScheduledExecutorService,
                                              OutboxProperties properties, OutboxDlqTransfer transfer) {
        OutboxProperties.DlqProperties dlqProperties = properties.getDlq();
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
    public OutboxMetrics outboxDlqMetrics(OutboxProperties properties, MeterRegistry registry, OutboxDlqManager manager) {
        return new OutboxDlqMetrics(registry, properties, manager);
    }
}
