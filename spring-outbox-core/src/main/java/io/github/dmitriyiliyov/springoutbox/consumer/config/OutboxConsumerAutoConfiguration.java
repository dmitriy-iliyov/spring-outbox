package io.github.dmitriyiliyov.springoutbox.consumer.config;

import io.github.dmitriyiliyov.springoutbox.consumer.*;
import io.github.dmitriyiliyov.springoutbox.publisher.core.OutboxScheduler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ConditionalOnProperty(prefix = "outbox.consumer", name = "enabled", havingValue = "true")
public class OutboxConsumerAutoConfiguration {

    private final OutboxConsumerProperties properties;

    public OutboxConsumerAutoConfiguration(OutboxConsumerProperties properties) {
        this.properties = properties;
    }

    @Bean
    public OutboxRepository outboxRepository(DataSource dataSource) {
        return OutboxRepositoryFactory.generate(dataSource);
    }

    @Bean
    public OutboxManager outboxManager(CacheManager cacheManager, OutboxRepository repository) {
        return new DefaultOutboxManager(cacheManager, repository);
    }

    @Bean
    public OutboxIdempotentWrapper outboxIdempotentWrapper(OutboxManager outboxManager, TransactionTemplate transactionTemplate) {
        return new DefaultOutboxIdempotentWrapper(transactionTemplate, outboxManager);
    }

    @Bean
    public OutboxScheduler outboxConsumedCleanUpScheduler(@Qualifier("outboxScheduledExecutorService")ScheduledExecutorService executor,
                                                          OutboxManager outboxManager) {
        return new OutboxConsumedCleanUpScheduler(properties.getCleanUp(), executor, outboxManager);
    }
}
