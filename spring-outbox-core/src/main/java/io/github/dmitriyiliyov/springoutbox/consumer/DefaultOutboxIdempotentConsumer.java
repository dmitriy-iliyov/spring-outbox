package io.github.dmitriyiliyov.springoutbox.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

public class DefaultOutboxIdempotentConsumer<T> implements OutboxIdempotentConsumer<T> {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxIdempotentConsumer.class);

    private final OutboxEventIdResolver<T> idResolver;
    private final TransactionTemplate transactionTemplate;
    private final ConsumedOutboxManager consumedOutboxManager;

    public DefaultOutboxIdempotentConsumer(OutboxEventIdResolver<T> idResolver, TransactionTemplate transactionTemplate,
                                           ConsumedOutboxManager consumedOutboxManager) {
        this.idResolver = idResolver;
        this.transactionTemplate = transactionTemplate;
        this.consumedOutboxManager = consumedOutboxManager;
    }

    @Override
    public void consume(T message, Runnable runnable) {
        UUID eventId = idResolver.resolve(message);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                if (!consumedOutboxManager.isConsumed(eventId)) {
                    runnable.run();
                }
            });
        } catch (Exception e) {
            log.error("Failed check idempotence and execute runnable", e);
            throw new RuntimeException(e);
        }
    }
}
