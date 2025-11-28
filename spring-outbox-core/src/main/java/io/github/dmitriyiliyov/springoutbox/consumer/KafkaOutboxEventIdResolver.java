package io.github.dmitriyiliyov.springoutbox.consumer;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxConstants;
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
                .filter(header -> header.key().equals(OutboxConstants.EVENT_ID_HEADER.getValue()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Header '%s' not found; cannot resolve"
                        .formatted(OutboxConstants.EVENT_ID_HEADER.getValue()))
                );
        return UUID.fromString(new String(eventIdHeader.value(), StandardCharsets.UTF_8));
    }

    @Override
    public Class<?> getSupports() {
        return ConsumerRecord.class;
    }
}