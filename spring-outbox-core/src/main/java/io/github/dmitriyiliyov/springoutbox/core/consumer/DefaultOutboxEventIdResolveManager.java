package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultOutboxEventIdResolveManager implements OutboxEventIdResolveManager {

    private final Map<Class<?>, OutboxEventIdResolver<?>> resolvers;

    public DefaultOutboxEventIdResolveManager(List<OutboxEventIdResolver<?>> resolvers) {
        this.resolvers = Collections.unmodifiableMap(
                resolvers.stream()
                        .collect(Collectors.toMap(OutboxEventIdResolver::getSupports, Function.identity()))
        );
    }

    @Override
    public <T> UUID resolve(T rowMessage) {
        OutboxEventIdResolver<?> resolver = resolvers.get(rowMessage.getClass());
        if (resolver == null) {
            throw new IllegalArgumentException(
                    "Unsupported class '%s'; cannot resolve".formatted(rowMessage.getClass().getName())
            );
        }
        return ((OutboxEventIdResolver<T>) resolver).resolve(rowMessage);
    }

    @Override
    public <T> Map<UUID, T> resolve(List<T> rowMessages) {
        if (rowMessages.isEmpty()) {
            return Collections.emptyMap();
        }
        T exampleRowMessage = rowMessages.getFirst();
        OutboxEventIdResolver<?> resolver = resolvers.get(exampleRowMessage.getClass());
        if (resolver == null) {
            throw new IllegalArgumentException(
                    "Unsupported class '%s'; cannot resolve".formatted(exampleRowMessage.getClass().getName())
            );
        }
        final OutboxEventIdResolver<T> finalResolver = (OutboxEventIdResolver<T>) resolver;
        return rowMessages.stream()
                .collect(Collectors.toMap(finalResolver::resolve, Function.identity()));
    }
}
