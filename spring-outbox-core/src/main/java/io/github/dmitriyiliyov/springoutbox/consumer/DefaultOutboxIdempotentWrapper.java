package io.github.dmitriyiliyov.springoutbox.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

public class DefaultOutboxIdempotentWrapper implements OutboxIdempotentWrapper {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxIdempotentWrapper.class);

    private final TransactionTemplate transactionTemplate;
    private final OutboxManager outboxManager;

    public DefaultOutboxIdempotentWrapper(TransactionTemplate transactionTemplate, OutboxManager outboxManager) {
        this.transactionTemplate = transactionTemplate;
        this.outboxManager = outboxManager;
    }

    @Override
    public void process(UUID eventId, Runnable runnable) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                if (outboxManager.saveIfAbsent(eventId)) {
                    runnable.run();
                }
            });
        } catch (Exception e) {
            log.error("Failed check idempotence and execute runnable", e);
            throw new RuntimeException(e);
        }
    }
}
