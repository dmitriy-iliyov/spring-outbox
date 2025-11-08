package io.github.dmitriyiliyov.springoutbox.consumer;

import java.util.UUID;

public interface OutboxIdempotentWrapper {
    void process(UUID eventId, Runnable delegate);
}
