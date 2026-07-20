package io.github.dmitriyiliyov.oncebox.core.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultOutboxIdempotentConsumer implements OutboxIdempotentConsumer {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxIdempotentConsumer.class);

    private final TransactionTemplate transactionTemplate;
    private final ConsumedOutboxManager consumedOutboxManager;

    public DefaultOutboxIdempotentConsumer(TransactionTemplate transactionTemplate,
                                           ConsumedOutboxManager consumedOutboxManager) {
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate cannot be null");
        this.consumedOutboxManager = Objects.requireNonNull(consumedOutboxManager, "consumedOutboxManager cannot be null");
    }

    @Override
    public void consume(UUID eventId, Runnable operation) {
        Objects.requireNonNull(eventId, "eventId cannot be null");
        Objects.requireNonNull(operation, "operation cannot be null");
        try {
            transactionTemplate.executeWithoutResult(status -> {
                if (consumedOutboxManager.tryConsume(eventId)) {
                    operation.run();
                }
            });
        } catch (Exception e) {
            log.error("Failed to check idempotency and execute operation for eventId: {}", eventId, e);
            throw e;
        }
    }

    @Override
    public <T> void consume(T message, OutboxEventIdExtractor<T> idExtractor, Consumer<T> operation) {
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(idExtractor, "idExtractor cannot be null");
        Objects.requireNonNull(operation, "operation cannot be null");
        UUID eventId = idExtractor.extract(message);
        Objects.requireNonNull(eventId, "eventId cannot be null");

        try {
            transactionTemplate.executeWithoutResult(status -> {
                if (consumedOutboxManager.tryConsume(eventId)) {
                    operation.accept(message);
                }
            });
        } catch (Exception e) {
            log.error("Failed to check idempotency and execute operation for eventId: {}", eventId, e);
            throw e;
        }
    }

    @Override
    public void consume(Set<UUID> ids, Consumer<Set<UUID>> operation) {
        if (ids == null || ids.isEmpty()) {
            log.warn("Provided ids set is null or empty");
            return;
        }
        Objects.requireNonNull(operation, "operation cannot be null");

        try {
            transactionTemplate.executeWithoutResult(status -> {
                Set<UUID> alreadyConsumedIds = consumedOutboxManager.tryConsumeAndGetDuplicates(ids);
                Set<UUID> validIds = new HashSet<>(ids);
                alreadyConsumedIds.forEach(validIds::remove);
                if (validIds.isEmpty()) {
                    log.info("Ids set is empty after filtering out duplicates");
                    return;
                }
                operation.accept(validIds);
            });
        } catch(Exception e) {
            log.error("Failed to check batch idempotency and execute operation for ids: {}", ids, e);
            throw e;
        }
    }

    @Override
    public <T> void consume(List<T> messages, OutboxEventIdExtractor<T> idExtractor, Consumer<List<T>> operation) {
        if (messages == null || messages.isEmpty()) {
            log.warn("Provided messages set is null or empty");
            return;
        }
        Objects.requireNonNull(idExtractor, "idExtractor cannot be null");
        Objects.requireNonNull(operation, "operation cannot be null");

        Map<UUID, T> messageMap = messages.stream()
                .collect(Collectors.toMap(
                        idExtractor::extract,
                        Function.identity(),
                        (existing, recipient) -> existing)
                );
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Set<UUID> alreadyConsumedIds = consumedOutboxManager.tryConsumeAndGetDuplicates(messageMap.keySet());
                alreadyConsumedIds.forEach(messageMap::remove);
                if (messageMap.isEmpty()) {
                    log.info("Messages set is empty after filtering out duplicates");
                    return;
                }
                operation.accept(new ArrayList<>(messageMap.values()));
            });
        } catch(Exception e) {
            log.error("Failed to check batch idempotency and execute operation for message ids: {}", messageMap.keySet(), e);
            throw e;
        }
    }
}
