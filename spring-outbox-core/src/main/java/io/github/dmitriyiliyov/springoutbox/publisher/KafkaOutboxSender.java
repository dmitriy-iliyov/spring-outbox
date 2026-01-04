package io.github.dmitriyiliyov.springoutbox.publisher;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxHeaders;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.SenderResult;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.CacheableClassResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class KafkaOutboxSender implements OutboxSender {

    private static final Logger log = LoggerFactory.getLogger(KafkaOutboxSender.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final long emergencyTimeout;
    private final ObjectMapper mapper;
    private final CacheableClassResolver classResolver;

    public KafkaOutboxSender(KafkaTemplate<String, Object> kafkaTemplate, long emergencyTimeout, ObjectMapper mapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.emergencyTimeout = emergencyTimeout;
        this.mapper = mapper;
        this.classResolver = new CacheableClassResolver();
    }

    @Override
    public SenderResult sendEvents(String topic, List<OutboxEvent> events) {
        if (events == null || events.isEmpty()) {
            return SenderResult.empty();
        }
        Set<UUID> processedIds = ConcurrentHashMap.newKeySet();
        Set<UUID> failedIds = ConcurrentHashMap.newKeySet();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (OutboxEvent event : events) {
            try {
                Message<?> message = MessageBuilder
                        .withPayload(mapper.readValue(event.getPayload(), classResolver.resolve(event.getPayloadType())))
                        .setHeader(KafkaHeaders.TOPIC, topic)
                        .setHeader(OutboxHeaders.EVENT_TYPE.getValue(), event.getEventType())
                        .setHeader(OutboxHeaders.EVENT_ID.getValue(), event.getId().toString())
                        .build();
                CompletableFuture<Void> future = kafkaTemplate
                        .send(message)
                        .thenAccept(success -> processedIds.add(event.getId()))
                        .exceptionally(ex -> {
                            failedIds.add(event.getId());
                            log.error("Error when sending event with id={} to topic={} ", event.getId(), topic, ex);
                            return null;
                        });
                futures.add(future);
            } catch (JsonParseException e) {
                failedIds.add(event.getId());
                log.error("Error when parsing event payload with id={} ", event.getId(), e);
            } catch (Exception e) {
                failedIds.add(event.getId());
                log.error("Error when sending event with id={} to topic={} ", event.getId(), topic, e);
            }
        }
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .orTimeout(emergencyTimeout, TimeUnit.SECONDS)
                    .join();
        } catch (CompletionException e) {
            events.stream()
                    .filter(event -> !processedIds.contains(event.getId()) && !failedIds.contains(event.getId()))
                    .forEach(event -> failedIds.add(event.getId()));
        }
        return new SenderResult(new HashSet<>(processedIds), new HashSet<>(failedIds));
    }
}
