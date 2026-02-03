package io.github.dmitriyiliyov.springoutbox.core.publisher;

public class OutboxSerializationException extends RuntimeException {
    public OutboxSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
