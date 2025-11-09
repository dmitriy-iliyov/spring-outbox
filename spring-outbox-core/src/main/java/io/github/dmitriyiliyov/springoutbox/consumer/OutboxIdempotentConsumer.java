package io.github.dmitriyiliyov.springoutbox.consumer;

import java.util.UUID;

public interface OutboxIdempotentConsumer {
    void consume(UUID eventId, Runnable delegate);
}
