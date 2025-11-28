package io.github.dmitriyiliyov.springoutbox.consumer;

import java.util.List;
import java.util.function.Consumer;

public interface OutboxIdempotentConsumer {
    <T> void consume(T message, Runnable operation);
    <T> void consume(List<T> messages, Consumer<List<T>> operation);
}
