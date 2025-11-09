package io.github.dmitriyiliyov.springoutbox.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxConstants;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.SenderResult;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.CacheableClassResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class KafkaOutboxSender implements OutboxSender {

    private static final Logger log = LoggerFactory.getLogger(KafkaOutboxSender.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper mapper;
    private final CacheableClassResolver classResolver;

    public KafkaOutboxSender(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper mapper) {
        this.kafkaTemplate = kafkaTemplate;
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
                        .setHeader(OutboxConstants.EVENT_ID_HEADER.getValue(), event.getId().toString())
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
            } catch (Exception e) {
                log.error("Error when sending event with id={} to topic={} ", event.getId(), topic, e);
                failedIds.add(event.getId());
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return new SenderResult(processedIds, failedIds);
    }
}
