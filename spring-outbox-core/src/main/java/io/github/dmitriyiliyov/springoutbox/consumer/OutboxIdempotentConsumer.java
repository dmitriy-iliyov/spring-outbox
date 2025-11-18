package io.github.dmitriyiliyov.springoutbox.consumer;

public interface OutboxIdempotentConsumer<T> {
    void consume(T message, Runnable delegate);
}
