package io.github.dmitriyiliyov.springoutbox.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

public class DefaultOutboxIdempotentConsumer implements OutboxIdempotentConsumer {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxIdempotentConsumer.class);

    private final TransactionTemplate transactionTemplate;
    private final ConsumedOutboxManager consumedOutboxManager;

    public DefaultOutboxIdempotentConsumer(TransactionTemplate transactionTemplate, ConsumedOutboxManager consumedOutboxManager) {
        this.transactionTemplate = transactionTemplate;
        this.consumedOutboxManager = consumedOutboxManager;
    }

    @Override
    public void consume(UUID eventId, Runnable runnable) {
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
