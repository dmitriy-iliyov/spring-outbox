package io.github.dmitriyiliyov.springoutbox;

import io.github.dmitriyiliyov.springoutbox.core.OutboxSender;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.domain.SenderType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class KafkaOutboxSender implements OutboxSender {
    @Override
    public Set<UUID> sendEvents(List<OutboxEvent> events) {
        return Set.of();
    }
}
