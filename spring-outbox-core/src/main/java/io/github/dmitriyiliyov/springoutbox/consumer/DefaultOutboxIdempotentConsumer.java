package io.github.dmitriyiliyov.springoutbox.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Consumer;

public class DefaultOutboxIdempotentConsumer implements OutboxIdempotentConsumer {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxIdempotentConsumer.class);

    private final OutboxEventIdResolveManager resolvingManager;
    private final TransactionTemplate transactionTemplate;
    private final ConsumedOutboxManager consumedOutboxManager;

    public DefaultOutboxIdempotentConsumer(OutboxEventIdResolveManager resolvingManager,
                                           TransactionTemplate transactionTemplate,
                                           ConsumedOutboxManager consumedOutboxManager) {
        this.resolvingManager = resolvingManager;
        this.transactionTemplate = transactionTemplate;
        this.consumedOutboxManager = consumedOutboxManager;
    }

    @Override
    public <T> void consume(T message, Runnable operation) {
        UUID eventId = resolvingManager.resolve(message);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                if (!consumedOutboxManager.isConsumed(eventId)) {
                    operation.run();
                }
            });
        } catch (Exception e) {
            log.error("Failed check idempotence and execute operation", e);
            throw e;
        }
    }

    @Override
    public <T> void consume(List<T> messages, Consumer<List<T>> operation) {
        Map<UUID, T> messageMap = resolvingManager.resolve(messages);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Set<UUID> alreadyConsumedIds = consumedOutboxManager.filterConsumed(messageMap.keySet());
                alreadyConsumedIds.forEach(messageMap::remove);
                if (!messageMap.isEmpty()) {
                    operation.accept(new ArrayList<>(messageMap.values()));
                }
            });
        } catch(Exception e) {
            log.error("Failed check batch idempotence and execute operation", e);
            throw e;
        }
    }
}
