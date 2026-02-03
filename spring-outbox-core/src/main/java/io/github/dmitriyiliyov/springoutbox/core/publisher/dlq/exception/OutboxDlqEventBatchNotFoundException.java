package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.exception;

import java.util.Set;
import java.util.UUID;

public class OutboxDlqEventBatchNotFoundException extends BadRequestException {

    private final Set<UUID> notFoundIds;

    public OutboxDlqEventBatchNotFoundException(Set<UUID> notFoundIds) {
        this.notFoundIds = notFoundIds;
    }

    @Override
    public String getDetail() {
        return "Outbox DLQ events with ids=%s not found, operation unavailable".formatted(notFoundIds);
    }

    public Set<UUID> getNotFoundIds() {
        return notFoundIds;
    }
}
