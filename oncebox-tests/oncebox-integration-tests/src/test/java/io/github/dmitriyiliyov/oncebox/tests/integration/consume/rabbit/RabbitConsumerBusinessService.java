package io.github.dmitriyiliyov.oncebox.tests.integration.consume.rabbit;

import io.github.dmitriyiliyov.oncebox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.messaging.OutboxHeadersUtils;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.ConsumerBusinessRepository;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class RabbitConsumerBusinessService {

    public static final String SINGLE_QUEUE = "test.outbox.single";
    public static final String BATCH_QUEUE  = "test.outbox.batch";
    public static final String SINGLE_ID_QUEUE = "test.outbox.single.id";
    public static final String BATCH_ID_QUEUE = "test.outbox.batch.id";

    private final OutboxIdempotentConsumer outboxConsumer;
    private final ConsumerBusinessRepository repository;

    public RabbitConsumerBusinessService(OutboxIdempotentConsumer outboxConsumer,
                                         ConsumerBusinessRepository repository) {
        this.outboxConsumer = outboxConsumer;
        this.repository = repository;
    }

    @Transactional
    @RabbitListener(
            queues = SINGLE_QUEUE,
            containerFactory = "testSingleRabbitListenerContainerFactory"
    )
    public void listenSingleMessage(Message<BusinessEvent> message) {
        outboxConsumer.consume(
                message,
                OutboxHeadersUtils::extractId,
                msg -> repository.save(msg.getPayload())
        );
    }

    @Transactional
    @RabbitListener(
            queues = BATCH_QUEUE,
            containerFactory = "testBatchRabbitListenerContainerFactory"
    )
    public void listenBatchMessages(List<Message<BusinessEvent>> messages) {
        outboxConsumer.consume(
                messages,
                OutboxHeadersUtils::extractId,
                deduped -> repository.saveAll(
                        deduped.stream().map(Message::getPayload).toList()
                )
        );
    }

    @Transactional
    @RabbitListener(
            queues = SINGLE_ID_QUEUE,
            containerFactory = "testSingleRabbitListenerContainerFactory"
    )
    public void listenSingleId(Message<BusinessEvent> message) {
        UUID eventId = OutboxHeadersUtils.extractId(message);
        outboxConsumer.consume(
                eventId,
                () -> repository.save(message.getPayload())
        );
    }

    @Transactional
    @RabbitListener(
            queues = BATCH_ID_QUEUE,
            containerFactory = "testBatchRabbitListenerContainerFactory"
    )
    public void listenBatchIds(List<Message<BusinessEvent>> messages) {
        Set<UUID> allIds = messages.stream()
                .map(OutboxHeadersUtils::extractId)
                .collect(Collectors.toSet());

        outboxConsumer.consume(
                allIds,
                newIds -> {
                    List<BusinessEvent> eventsToSave = messages.stream()
                            .filter(msg -> newIds.contains(OutboxHeadersUtils.extractId(msg)))
                            .map(Message::getPayload)
                            .toList();
                    repository.saveAll(eventsToSave);
                }
        );
    }
}