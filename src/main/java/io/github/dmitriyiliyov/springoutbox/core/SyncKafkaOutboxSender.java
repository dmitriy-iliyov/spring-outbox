package io.github.dmitriyiliyov.springoutbox.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.domain.SenderResult;
import io.github.dmitriyiliyov.springoutbox.utils.ClassResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SyncKafkaOutboxSender implements OutboxSender {

    private static final Logger log = LoggerFactory.getLogger(SyncKafkaOutboxSender.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper mapper;
    private final ClassResolver classResolver;

    public SyncKafkaOutboxSender(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper mapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
        this.classResolver = new ClassResolver();
    }

    @Override
    public SenderResult sendEvents(String topic, List<OutboxEvent> events) {
        Set<UUID> processedIds = new HashSet<>();
        Set<UUID> failedIds = new HashSet<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (OutboxEvent event : events) {
            try {
                CompletableFuture<Void> future = kafkaTemplate
                        .send(topic, mapper.readValue(event.getPayload(), classResolver.resolve(event.getPayloadType())))
                        .thenAccept(success -> processedIds.add(event.getId()))
                        .exceptionally(ex -> {
                            failedIds.add(event.getId());
                            log.error("Error when sending event with id={} to topic={}, ", event.getId(), topic, ex);
                            return null;
                        });
                futures.add(future);
            } catch (Exception e) {
                log.error("Error when sending event with id={} to topic={}, ", event.getId(), topic, e);
                failedIds.add(event.getId());
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return new SenderResult(processedIds, failedIds);
    }
}
