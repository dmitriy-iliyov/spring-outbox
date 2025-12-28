package io.github.dmitriyiliyov.springoutbox.publisher;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxHeaders;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.SenderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RabbitMqOutboxSender implements OutboxSender {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqOutboxSender.class);

    private final RabbitTemplate rabbitTemplate;
    private final long emergencyTimeout;

    public RabbitMqOutboxSender(RabbitTemplate rabbitTemplate, long emergencyTimeout) {
        this.rabbitTemplate = rabbitTemplate;
        this.emergencyTimeout = emergencyTimeout;
    }

    @Override
    public SenderResult sendEvents(String exchange, List<OutboxEvent> events) {
        Set<UUID> processedIds = ConcurrentHashMap.newKeySet();
        Set<UUID> failedIds = ConcurrentHashMap.newKeySet();
        List<CompletableFuture<CorrelationData.Confirm>> futures = new ArrayList<>();
        for (OutboxEvent event: events) {
            try {
                MessageProperties properties = MessagePropertiesBuilder.newInstance()
                        .setHeader(OutboxHeaders.EVENT_ID.getValue(), event.getId())
                        .setHeader(OutboxHeaders.EVENT_TYPE.getValue(), event.getEventType())
                        .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                        .build();
                Message message = MessageBuilder.withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                        .andProperties(properties)
                        .build();
                final CorrelationData correlationData = new CorrelationData(event.getId().toString());
                rabbitTemplate.send(exchange, event.getEventType(), message, correlationData);
                CompletableFuture<CorrelationData.Confirm> future = correlationData.getFuture()
                        .thenApply(success -> {
                            if (success.isAck() && correlationData.getReturned() == null) {
                                processedIds.add(event.getId());
                            } else {
                                failedIds.add(event.getId());
                                log.error(
                                        "Broker didn't acknowledge receipt event with id={} exchange={}. Reason: {}. Returned message: {}",
                                        event.getId(), exchange, success.getReason(), correlationData.getReturned()
                                );
                            }
                            return success;
                        })
                        .exceptionally(ex -> {
                            failedIds.add(event.getId());
                            log.error("Error when sending event with id={} to exchange={} ", event.getId(), exchange, ex);
                            return null;
                        });
                futures.add(future);
            } catch (Exception e) {
                failedIds.add(event.getId());
                log.error("Error when sending event with id={} to exchange={} ", event.getId(), exchange, e);
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(emergencyTimeout, TimeUnit.SECONDS)
                .join();
        return new SenderResult(new HashSet<>(processedIds), new HashSet<>(failedIds));
    }
}
