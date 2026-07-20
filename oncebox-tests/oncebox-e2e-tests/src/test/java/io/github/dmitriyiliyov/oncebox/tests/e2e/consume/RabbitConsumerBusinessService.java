package io.github.dmitriyiliyov.oncebox.tests.e2e.consume;

import io.github.dmitriyiliyov.oncebox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.messaging.OutboxHeadersUtils;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.BusinessEvent;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.E2eEvents;
import io.github.dmitriyiliyov.oncebox.tests.e2e.repository.TestOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.Message;

public class RabbitConsumerBusinessService {

    private static final Logger log = LoggerFactory.getLogger(RabbitConsumerBusinessService.class);

    private final OutboxIdempotentConsumer outboxConsumer;
    private final TestOutboxRepository repository;

    public RabbitConsumerBusinessService(OutboxIdempotentConsumer outboxConsumer, TestOutboxRepository repository) {
        this.outboxConsumer = outboxConsumer;
        this.repository = repository;
    }

    @RabbitListener(queues = E2eEvents.QUEUE, containerFactory = "outboxRabbitListenerContainerFactory")
    public void listen(Message<BusinessEvent> message) {
        try {
            outboxConsumer.consume(
                    message,
                    OutboxHeadersUtils::extractId,
                    msg -> repository.saveConsumedBusiness(msg.getPayload().verifyId())
            );
        } catch (Exception e) {
            log.error("Error when consuming message", e);
            throw e;
        }
    }
}
