package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.util.UUID;

public interface OutboxEventIdResolver<T> {
    UUID resolve(T rowMessage);
    Class<?> getSupports();
}
