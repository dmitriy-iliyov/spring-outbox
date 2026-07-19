package io.github.dmitriyiliyov.springoutbox.tests.e2e.consume;

import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.messaging.OutboxHeadersUtils;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.domain.BusinessEvent;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.domain.E2eEvents;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.repository.TestOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.Message;

public class ConsumerBusinessService {

    public static final String CONSUMER_GROUP = "e2e-consumer-group";

    private static final Logger log = LoggerFactory.getLogger(ConsumerBusinessService.class);

    private final OutboxIdempotentConsumer outboxConsumer;
    private final TestOutboxRepository repository;

    public ConsumerBusinessService(OutboxIdempotentConsumer outboxConsumer, TestOutboxRepository repository) {
        this.outboxConsumer = outboxConsumer;
        this.repository = repository;
    }

    @KafkaListener(topics = E2eEvents.TOPIC, groupId = CONSUMER_GROUP, containerFactory = "outboxKafkaListenerContainerFactory")
    public void listen(Message<BusinessEvent> message, Acknowledgment ack) {
        try {
            outboxConsumer.consume(
                    message,
                    OutboxHeadersUtils::extractId,
                    msg -> repository.saveConsumedBusiness(msg.getPayload().verifyId())
            );
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error when consuming message", e);
            throw e;
        }
    }
}
