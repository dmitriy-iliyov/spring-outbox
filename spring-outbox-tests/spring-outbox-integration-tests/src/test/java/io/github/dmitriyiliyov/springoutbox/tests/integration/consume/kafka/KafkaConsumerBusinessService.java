package io.github.dmitriyiliyov.springoutbox.tests.integration.consume.kafka;

import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.messaging.OutboxHeadersUtils;
import io.github.dmitriyiliyov.springoutbox.tests.integration.consume.shared.ConsumerBusinessRepository;
import io.github.dmitriyiliyov.springoutbox.tests.integration.domain.BusinessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class KafkaConsumerBusinessService {

    public static final String SINGLE_TOPIC = "test.outbox.single";
    public static final String BATCH_TOPIC = "test.outbox.batch";
    public static final String SINGLE_ID_TOPIC = "test.outbox.single.id";
    public static final String BATCH_ID_TOPIC = "test.outbox.batch.id";
    public static final String CONSUMER_GROUP = "test-outbox-consumer";

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerBusinessService.class);

    private final OutboxIdempotentConsumer outboxConsumer;
    private final ConsumerBusinessRepository repository;

    public KafkaConsumerBusinessService(OutboxIdempotentConsumer outboxConsumer,
                                        ConsumerBusinessRepository repository) {
        this.outboxConsumer = outboxConsumer;
        this.repository = repository;
    }

    @KafkaListener(topics = SINGLE_TOPIC, groupId = CONSUMER_GROUP, containerFactory = "testSingleKafkaListenerContainerFactory")
    public void listenSingleMessage(Message<BusinessEvent> message, Acknowledgment ack) {
        try {
            outboxConsumer.consume(
                    message,
                    OutboxHeadersUtils::extractId,
                    msg -> repository.save(msg.getPayload())
            );
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error when consuming single message", e);
            throw e;
        }
    }

    @KafkaListener(topics = BATCH_TOPIC, groupId = CONSUMER_GROUP, containerFactory = "testBatchKafkaListenerContainerFactory")
    public void listenBatchMessages(List<Message<BusinessEvent>> messages, Acknowledgment ack) {
        try {
            outboxConsumer.consume(
                    messages,
                    OutboxHeadersUtils::extractId,
                    dedupedMessages -> repository.saveAll(
                            dedupedMessages.stream().map(Message::getPayload).toList()
                    )
            );
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error when consuming batch of messages", e);
            throw e;
        }
    }

    @KafkaListener(topics = SINGLE_ID_TOPIC, groupId = CONSUMER_GROUP, containerFactory = "testSingleKafkaListenerContainerFactory")
    public void listenSingleId(Message<BusinessEvent> message, Acknowledgment ack) {
        try {
            UUID eventId = OutboxHeadersUtils.extractId(message);
            outboxConsumer.consume(
                    eventId,
                    () -> repository.save(message.getPayload())
            );
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error when consuming single ID", e);
            throw e;
        }
    }

    @KafkaListener(topics = BATCH_ID_TOPIC, groupId = CONSUMER_GROUP, containerFactory = "testBatchKafkaListenerContainerFactory")
    public void listenBatchIds(List<Message<BusinessEvent>> messages, Acknowledgment ack) {
        try {
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
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error when consuming batch of IDs", e);
            throw e;
        }
    }
}
