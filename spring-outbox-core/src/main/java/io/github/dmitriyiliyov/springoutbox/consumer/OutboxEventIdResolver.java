package io.github.dmitriyiliyov.springoutbox.consumer;

import java.util.UUID;

public interface OutboxEventIdResolver<T> {
    UUID resolve(T rowMessage);
    boolean supports(Class<?> c);
}
