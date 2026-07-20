package io.github.dmitriyiliyov.oncebox.tests.integration.consume.kafka;

import io.github.dmitriyiliyov.oncebox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.messaging.OutboxHeadersUtils;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.ConsumerBusinessRepository;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class KafkaConsumerFaultyBusinessService {

    public static final String SINGLE_FAILING_TOPIC = "test.outbox.single.failing";
    public static final String BATCH_FAILING_TOPIC  = "test.outbox.batch.failing";
    public static final String SINGLE_ID_FAILING_TOPIC = "test.outbox.single.id.failing";
    public static final String BATCH_ID_FAILING_TOPIC = "test.outbox.batch.id.failing";
    public static final String CONSUMER_GROUP = "test-outbox-faulty-consumer";

    private final OutboxIdempotentConsumer outboxConsumer;
    private final ConsumerBusinessRepository repository;
    private final AtomicBoolean shouldFail = new AtomicBoolean(true);

    public KafkaConsumerFaultyBusinessService(OutboxIdempotentConsumer outboxConsumer,
                                              ConsumerBusinessRepository repository) {
        this.outboxConsumer = outboxConsumer;
        this.repository = repository;
    }

    public void setShouldFail(boolean fail) {
        shouldFail.set(fail);
    }

    @KafkaListener(topics = SINGLE_FAILING_TOPIC, groupId = CONSUMER_GROUP, containerFactory = "testSingleKafkaListenerContainerFactory")
    public void listenFailing(Message<BusinessEvent> message, Acknowledgment ack) {
        try {
            outboxConsumer.consume(
                    message,
                    OutboxHeadersUtils::extractId,
                    msg -> {
                        if (shouldFail.get()) {
                            throw new RuntimeException("Exception in business operation");
                        }
                        repository.save(msg.getPayload());
                    }
            );
            ack.acknowledge();
        } catch (Exception e) {
            throw e;
        }
    }

    @KafkaListener(topics = BATCH_FAILING_TOPIC, groupId = CONSUMER_GROUP, containerFactory = "testBatchKafkaListenerContainerFactory")
    public void listenBatchFailing(List<Message<BusinessEvent>> messages, Acknowledgment ack) {
        try {
            outboxConsumer.consume(
                    messages,
                    OutboxHeadersUtils::extractId,
                    deduped -> {
                        if (shouldFail.get()) {
                            throw new RuntimeException("Exception in business operation");
                        }
                        repository.saveAll(deduped.stream().map(Message::getPayload).toList());
                    }
            );
            ack.acknowledge();
        } catch (Exception e) {
            throw e;
        }
    }

    @KafkaListener(topics = SINGLE_ID_FAILING_TOPIC, groupId = CONSUMER_GROUP, containerFactory = "testSingleKafkaListenerContainerFactory")
    public void listenSingleIdFailing(Message<BusinessEvent> message, Acknowledgment ack) {
        try {
            UUID eventId = OutboxHeadersUtils.extractId(message);
            outboxConsumer.consume(
                    eventId,
                    () -> {
                        if (shouldFail.get()) {
                            throw new RuntimeException("Exception in business operation");
                        }
                        repository.save(message.getPayload());
                    }
            );
            ack.acknowledge();
        } catch (Exception e) {
            throw e;
        }
    }

    @KafkaListener(topics = BATCH_ID_FAILING_TOPIC, groupId = CONSUMER_GROUP, containerFactory = "testBatchKafkaListenerContainerFactory")
    public void listenBatchIdsFailing(List<Message<BusinessEvent>> messages, Acknowledgment ack) {
        try {
            Set<UUID> allIds = messages.stream()
                    .map(OutboxHeadersUtils::extractId)
                    .collect(Collectors.toSet());

            outboxConsumer.consume(
                    allIds,
                    newIds -> {
                        if (shouldFail.get()) {
                            throw new RuntimeException("Exception in business operation");
                        }
                        List<BusinessEvent> eventsToSave = messages.stream()
                                .filter(msg -> newIds.contains(OutboxHeadersUtils.extractId(msg)))
                                .map(Message::getPayload)
                                .toList();

                        repository.saveAll(eventsToSave);
                    }
            );
            ack.acknowledge();
        } catch (Exception e) {
            throw e;
        }
    }
}
