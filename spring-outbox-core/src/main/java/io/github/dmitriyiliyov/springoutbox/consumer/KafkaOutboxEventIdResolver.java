package io.github.dmitriyiliyov.springoutbox.consumer;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxHeaders;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class KafkaOutboxEventIdResolver implements OutboxEventIdResolver<ConsumerRecord<String, ?>> {

    @Override
    public UUID resolve(ConsumerRecord<String, ?> rowMessage) {
        Header [] headers = rowMessage.headers().toArray();
        Header eventIdHeader = Arrays.stream(headers)
                .filter(header -> header.key().equals(OutboxHeaders.EVENT_ID.getValue()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Header '%s' not found; cannot resolve"
                        .formatted(OutboxHeaders.EVENT_ID.getValue()))
                );
        return UUID.fromString(new String(eventIdHeader.value(), StandardCharsets.UTF_8));
    }

    @Override
    public Class<?> getSupports() {
        return ConsumerRecord.class;
    }
}