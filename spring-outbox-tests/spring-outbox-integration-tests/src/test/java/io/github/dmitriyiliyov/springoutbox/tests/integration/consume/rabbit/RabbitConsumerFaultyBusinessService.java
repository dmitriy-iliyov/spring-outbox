package io.github.dmitriyiliyov.springoutbox.tests.integration.consume.rabbit;

import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.messaging.OutboxHeadersUtils;
import io.github.dmitriyiliyov.springoutbox.tests.integration.consume.shared.ConsumerBusinessRepository;
import io.github.dmitriyiliyov.springoutbox.tests.integration.domain.BusinessEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class RabbitConsumerFaultyBusinessService {

    public static final String SINGLE_FAILING_QUEUE = "test.outbox.single.failing";
    public static final String BATCH_FAILING_QUEUE  = "test.outbox.batch.failing";
    public static final String SINGLE_ID_FAILING_QUEUE = "test.outbox.single.id.failing";
    public static final String BATCH_ID_FAILING_QUEUE = "test.outbox.batch.id.failing";

    private final OutboxIdempotentConsumer outboxConsumer;
    private final ConsumerBusinessRepository repository;
    private final AtomicBoolean shouldFail = new AtomicBoolean(true);

    public RabbitConsumerFaultyBusinessService(OutboxIdempotentConsumer outboxConsumer,
                                               ConsumerBusinessRepository repository) {
        this.outboxConsumer = outboxConsumer;
        this.repository = repository;
    }

    public void setShouldFail(boolean fail) {
        shouldFail.set(fail);
    }

    @Transactional
    @RabbitListener(
            queues = SINGLE_FAILING_QUEUE,
            containerFactory = "testSingleRabbitListenerContainerFactory"
    )
    public void listenFailing(Message<BusinessEvent> message) {
        outboxConsumer.consume(
                message,
                OutboxHeadersUtils::extractId,
                msg -> {
                    if (shouldFail.get()) {
                        throw new RuntimeException("Simulated business operation failure");
                    }
                    repository.save(msg.getPayload());
                }
        );
    }

    @Transactional
    @RabbitListener(
            queues = BATCH_FAILING_QUEUE,
            containerFactory = "testBatchRabbitListenerContainerFactory"
    )
    public void listenBatchFailing(List<Message<BusinessEvent>> messages) {
        outboxConsumer.consume(
                messages,
                OutboxHeadersUtils::extractId,
                deduped -> {
                    if (shouldFail.get()) {
                        throw new RuntimeException("Simulated batch business operation failure");
                    }
                    repository.saveAll(deduped.stream().map(Message::getPayload).toList());
                }
        );
    }

    @Transactional
    @RabbitListener(
            queues = SINGLE_ID_FAILING_QUEUE,
            containerFactory = "testSingleRabbitListenerContainerFactory"
    )
    public void listenSingleIdFailing(Message<BusinessEvent> message) {
        UUID eventId = OutboxHeadersUtils.extractId(message);
        outboxConsumer.consume(
                eventId,
                () -> {
                    if (shouldFail.get()) {
                        throw new RuntimeException("Simulated business operation failure");
                    }
                    repository.save(message.getPayload());
                }
        );
    }

    @Transactional
    @RabbitListener(
            queues = BATCH_ID_FAILING_QUEUE,
            containerFactory = "testBatchRabbitListenerContainerFactory"
    )
    public void listenBatchIdsFailing(List<Message<BusinessEvent>> messages) {
        Set<UUID> allIds = messages.stream()
                .map(OutboxHeadersUtils::extractId)
                .collect(Collectors.toSet());

        outboxConsumer.consume(
                allIds,
                newIds -> {
                    if (shouldFail.get()) {
                        throw new RuntimeException("Simulated batch business operation failure");
                    }
                    List<BusinessEvent> eventsToSave = messages.stream()
                            .filter(msg -> newIds.contains(OutboxHeadersUtils.extractId(msg)))
                            .map(Message::getPayload)
                            .toList();
                    repository.saveAll(eventsToSave);
                }
        );
    }
}