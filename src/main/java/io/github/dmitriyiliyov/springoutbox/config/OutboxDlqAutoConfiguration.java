package io.github.dmitriyiliyov.springoutbox.config;

import io.github.dmitriyiliyov.springoutbox.core.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.dlq.*;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxDlqMetrics;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.utils.DumbOutboxCache;
import io.github.dmitriyiliyov.springoutbox.utils.OutboxCache;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ConditionalOnProperty(prefix = "outbox.dlq", name = "enable", havingValue = "true")
public class OutboxDlqAutoConfiguration {

    @Bean
    public OutboxDlqRepository outboxRepository(DataSource dataSource) {
        return OutboxDlqRepositoryFactory.generate(dataSource);
    }

    @Bean
    public OutboxCache<DlqStatus> outboxDlqCache() {
        return new DumbOutboxCache<>(30, 30, 30);
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
        return new OutboxDlqScheduler(properties.getDlq(), outboxScheduledExecutorService, transfer);
    }

    @Bean
    public OutboxMetrics outboxDlqMetrics(OutboxProperties properties, MeterRegistry registry, OutboxDlqManager manager) {
        return new OutboxDlqMetrics(registry, properties, manager);
    }
}
