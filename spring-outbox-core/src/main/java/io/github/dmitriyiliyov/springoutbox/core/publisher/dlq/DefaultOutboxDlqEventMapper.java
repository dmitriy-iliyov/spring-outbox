package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;

import java.time.Clock;
import java.util.List;

public class DefaultOutboxDlqEventMapper implements OutboxDlqEventMapper {

    private final Clock clock;

    public DefaultOutboxDlqEventMapper(Clock clock) {
        this.clock = clock;
    }

    @Override
    public OutboxDlqEvent toDlqEvent(OutboxEvent event) {
        return new OutboxDlqEvent(
                event.getId(),
                EventStatus.FAILED,
                event.getEventType(),
                event.getPayloadType(),
                event.getPayload(),
                event.getRetryCount(),
                event.getNextRetryAt(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                DlqStatus.MOVED,
                clock.instant()
        );
    }

    @Override
    public List<OutboxDlqEvent> toDlqEvents(List<OutboxEvent> events) {
        return events.stream()
                .map(this::toDlqEvent)
                .toList();
    }

    @Override
    public OutboxEvent toOutboxEvent(OutboxDlqEvent event) {
        return new OutboxEvent(
                event.getId(),
                EventStatus.PENDING,
                event.getEventType(),
                event.getPayloadType(),
                event.getPayload(),
                -1,
                clock.instant(),
                event.getCreatedAt(),
                clock.instant()
        );
    }

    @Override
    public List<OutboxEvent> toOutboxEvents(List<OutboxDlqEvent> events) {
        return events.stream()
                .map(this::toOutboxEvent)
                .toList();
    }
}
