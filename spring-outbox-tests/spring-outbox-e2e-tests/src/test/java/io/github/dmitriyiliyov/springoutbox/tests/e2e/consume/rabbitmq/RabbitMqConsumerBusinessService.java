package io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.rabbitmq;

import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.ConsumerBusinessRepository;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.domain.BusinessEvent;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service simulating a real business RabbitMQ listener for E2E testing of
 * {@link io.github.dmitriyiliyov.springoutbox.core.consumer.DefaultOutboxIdempotentConsumer}.
 */
public class RabbitMqConsumerBusinessService {

    public static final String SINGLE_QUEUE = "test.outbox.single";
    public static final String BATCH_QUEUE  = "test.outbox.batch";

    private final OutboxIdempotentConsumer outboxConsumer;
    private final ConsumerBusinessRepository repository;
    private final MessageConverter converter;

    public RabbitMqConsumerBusinessService(OutboxIdempotentConsumer outboxConsumer,
                                           ConsumerBusinessRepository repository,
                                           MessageConverter converter) {
        this.outboxConsumer = outboxConsumer;
        this.repository = repository;
        this.converter = converter;
    }

    @Transactional
    @RabbitListener(
            queues = SINGLE_QUEUE,
            containerFactory = "testSingleRabbitListenerContainerFactory"
    )
    public void listen(Message message) {
        outboxConsumer.consume(message, () ->
                repository.save(converter.toEvent(message))
        );
    }

    @Transactional
    @RabbitListener(
            queues = BATCH_QUEUE,
            containerFactory = "testBatchRabbitListenerContainerFactory"
    )
    public void listenBatch(List<Message> messages) {
        outboxConsumer.consume(messages, (deduped) ->
                repository.saveAll(
                        deduped.stream().map(converter::toEvent).toList()
                )
        );
    }

    /**
     * Converts raw AMQP {@link Message} body to {@link BusinessEvent}.
     * Injected so JPA and JDBC variants can share the same listener class.
     */
    @FunctionalInterface
    public interface MessageConverter {
        BusinessEvent toEvent(Message message);
    }
}