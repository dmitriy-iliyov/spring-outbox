package io.github.dmitriyiliyov.springoutbox.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.domain.SenderResult;
import io.github.dmitriyiliyov.springoutbox.utils.ClassResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AsyncKafkaOutboxSender implements OutboxSender {

    private static final Logger log = LoggerFactory.getLogger(AsyncKafkaOutboxSender.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper mapper;
    private final ClassResolver classResolver;

    public AsyncKafkaOutboxSender(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper mapper, ClassResolver classResolver) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
        this.classResolver = new ClassResolver();
    }

    @Override
    public SenderResult sendEvents(String topic, List<OutboxEvent> events) {
        Set<UUID> processedIds = new HashSet<>();
        Set<UUID> failedIds = new HashSet<>();
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(topic, mapper.readValue(event.getPayload(), classResolver.resolve(event.getPayloadType())));
                processedIds.add(event.getId());
            } catch (Exception e) {
                log.error("Error when sending event with id={} to topic={}, ", event.getId(), topic, e);
                failedIds.add(event.getId());
            }
        }
        return new SenderResult(processedIds, failedIds);
    }
}
