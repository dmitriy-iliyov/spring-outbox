package io.github.dmitriyiliyov.springoutbox.consumer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OutboxEventIdResolvingManager<T> {
    UUID resolve(T rowMessage);

    Map<UUID, T> resolve(List<T> rowMessages);
}
