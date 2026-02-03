package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OutboxEventIdResolveManager {
    <T> UUID resolve(T rowMessage);
    <T> Map<UUID, T> resolve(List<T> rowMessages);
}
