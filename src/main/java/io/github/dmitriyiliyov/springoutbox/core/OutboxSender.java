package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.domain.SenderResult;

import java.util.List;

public interface OutboxSender {
    SenderResult sendEvents(String topic, List<OutboxEvent> events);
}
