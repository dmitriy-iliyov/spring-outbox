package io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.rabbitmq;

import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.ConsumerBusinessRepository;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RabbitMQ listener that simulates business operation failures for E2E transaction rollback tests.
 *
 * @see io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.kafka.KafkaConsumerFaultyBusinessService
 */
public class RabbitMqConsumerFaultyBusinessService {

    public static final String SINGLE_FAILING_QUEUE = "test.outbox.single.failing";
    public static final String BATCH_FAILING_QUEUE  = "test.outbox.batch.failing";

    private final OutboxIdempotentConsumer outboxConsumer;
    private final ConsumerBusinessRepository repository;
    private final RabbitMqConsumerBusinessService.MessageConverter converter;
    private final AtomicBoolean shouldFail = new AtomicBoolean(true);

    public RabbitMqConsumerFaultyBusinessService(OutboxIdempotentConsumer outboxConsumer,
                                                 ConsumerBusinessRepository repository,
                                                 RabbitMqConsumerBusinessService.MessageConverter converter) {
        this.outboxConsumer = outboxConsumer;
        this.repository = repository;
        this.converter = converter;
    }

    public void setShouldFail(boolean fail) {
        shouldFail.set(fail);
    }

    @Transactional
    @RabbitListener(
            queues = SINGLE_FAILING_QUEUE,
            containerFactory = "testSingleRabbitListenerContainerFactory"
    )
    public void listenFailing(Message message) {
        outboxConsumer.consume(message, () -> {
            if (shouldFail.get()) {
                throw new RuntimeException("Simulated business operation failure");
            }
            repository.save(converter.toEvent(message));
        });
    }

    @Transactional
    @RabbitListener(
            queues = BATCH_FAILING_QUEUE,
            containerFactory = "testBatchRabbitListenerContainerFactory"
    )
    public void listenBatchFailing(List<Message> messages) {
        outboxConsumer.consume(messages, (deduped) -> {
            if (shouldFail.get()) {
                throw new RuntimeException("Simulated batch business operation failure");
            }
            repository.saveAll(deduped.stream().map(converter::toEvent).toList());
        });
    }
}
