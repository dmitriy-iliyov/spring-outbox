package io.github.dmitriyiliyov.springoutbox.tests.integration.consume.kafka;

import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.tests.integration.consume.shared.ConsumerBusinessRepository;
import io.github.dmitriyiliyov.springoutbox.tests.integration.domain.BusinessEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

public class KafkaConsumerBusinessService {

    public static final String SINGLE_TOPIC = "test.outbox.single";
    public static final String BATCH_TOPIC = "test.outbox.batch";
    public static final String CONSUMER_GROUP = "test-outbox-consumer";
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerBusinessService.class);

    private final OutboxIdempotentConsumer outboxConsumer;
    private final ConsumerBusinessRepository repository;

    public KafkaConsumerBusinessService(OutboxIdempotentConsumer outboxConsumer,
                                        ConsumerBusinessRepository repository) {
        this.outboxConsumer = outboxConsumer;
        this.repository = repository;
    }

    @KafkaListener(
            topics = SINGLE_TOPIC,
            groupId = CONSUMER_GROUP,
            containerFactory = "testSingleKafkaListenerContainerFactory"
    )
    public void listen(ConsumerRecord<String, BusinessEvent> record, Acknowledgment ack) {
        try {
            outboxConsumer.consume(record, () -> repository.save(record.value()));
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error when consuming event", e);
            throw e;
        }
    }

    @KafkaListener(
            topics = BATCH_TOPIC,
            groupId = CONSUMER_GROUP,
            containerFactory = "testBatchKafkaListenerContainerFactory"
    )
    public void listenBatch(List<ConsumerRecord<String, BusinessEvent>> records, Acknowledgment ack) {
        try {
            outboxConsumer.consume(
                    records,
                    (deduped) -> repository.saveAll(
                            deduped.stream().map(ConsumerRecord::value).toList()
                    )
            );
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error when consuming event", e);
            throw e;
        }
    }
}