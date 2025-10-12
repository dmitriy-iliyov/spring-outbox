package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.domain.SenderType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OutboxSender {
    Set<UUID> sendEvents(List<OutboxEvent> events);
}
