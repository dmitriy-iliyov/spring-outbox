package io.github.dmitriyiliyov.oncebox.core.publisher.dlq;

import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;

import java.util.List;

public interface OutboxDlqEventMapper {
    OutboxDlqEvent toDlqEvent(OutboxEvent event);

    List<OutboxDlqEvent> toDlqEvents(List<OutboxEvent> events);

    OutboxEvent toOutboxEvent(OutboxDlqEvent event);

    List<OutboxEvent> toOutboxEvents(List<OutboxDlqEvent> events);
}
