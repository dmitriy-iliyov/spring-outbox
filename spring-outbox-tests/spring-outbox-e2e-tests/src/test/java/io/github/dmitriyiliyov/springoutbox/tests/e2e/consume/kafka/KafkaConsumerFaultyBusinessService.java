package io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.kafka;

import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.ConsumerBusinessRepository;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.domain.BusinessEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Kafka listener that simulates business operation failures for E2E transaction rollback tests.
 */
public class KafkaConsumerFaultyBusinessService {

    public static final String SINGLE_FAILING_TOPIC = "test.outbox.single.failing";
    public static final String BATCH_FAILING_TOPIC  = "test.outbox.batch.failing";
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

    @KafkaListener(
            topics = SINGLE_FAILING_TOPIC,
            groupId = CONSUMER_GROUP,
            containerFactory = "testSingleKafkaListenerContainerFactory"
    )
    public void listenFailing(ConsumerRecord<String, BusinessEvent> record, Acknowledgment ack) {
        try {
            outboxConsumer.consume(record, () -> {
                if (shouldFail.get()) {
                    throw new RuntimeException("Exception in business operation");
                }
                repository.save(record.value());
            });
            ack.acknowledge();
        } catch (Exception e) {
            throw e;
        }
    }

    @KafkaListener(
            topics = BATCH_FAILING_TOPIC,
            groupId = CONSUMER_GROUP,
            containerFactory = "testBatchKafkaListenerContainerFactory"
    )
    public void listenBatchFailing(List<ConsumerRecord<String, BusinessEvent>> records, Acknowledgment ack) {
        try {
            outboxConsumer.consume(records, (deduped) -> {
                if (shouldFail.get()) {
                    throw new RuntimeException("Exception in business operation");
                }
                repository.saveAll(deduped.stream().map(ConsumerRecord::value).toList());
            });
            ack.acknowledge();
        } catch (Exception e) {
            throw e;
        }
    }
}
