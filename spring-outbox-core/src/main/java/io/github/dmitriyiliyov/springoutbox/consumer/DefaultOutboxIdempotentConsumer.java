package io.github.dmitriyiliyov.springoutbox.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class DefaultOutboxIdempotentConsumer<T> implements OutboxIdempotentConsumer<T> {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxIdempotentConsumer.class);

    private final OutboxEventIdResolvingManager<T> resolvingManager;
    private final TransactionTemplate transactionTemplate;
    private final ConsumedOutboxManager consumedOutboxManager;

    public DefaultOutboxIdempotentConsumer(OutboxEventIdResolvingManager<T> resolvingManager,
                                           TransactionTemplate transactionTemplate,
                                           ConsumedOutboxManager consumedOutboxManager) {
        this.resolvingManager = resolvingManager;
        this.transactionTemplate = transactionTemplate;
        this.consumedOutboxManager = consumedOutboxManager;
    }

    @Override
    public void consume(T message, Runnable operation) {
        UUID eventId = resolvingManager.resolve(message);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                if (!consumedOutboxManager.isConsumed(eventId)) {
                    operation.run();
                }
            });
        } catch (Exception e) {
            log.error("Failed check idempotence and execute operation", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void consume(List<T> messages, Consumer<List<T>> operation) {
        Map<UUID, T> eventIds = resolvingManager.resolve(messages);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Set<UUID> pastConsumedIds = consumedOutboxManager.filterConsumed(eventIds.keySet());
                pastConsumedIds.forEach(eventIds::remove);
                operation.accept((List<T>) eventIds.values());
            });
        } catch(Exception e) {
            log.error("Failed check idempotence and execute operation", e);
            throw new RuntimeException(e);
        }
    }
}
