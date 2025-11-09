package io.github.dmitriyiliyov.springoutbox.consumer;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class OutboxEventIdResolverManager<T> implements OutboxEventIdResolver<T>{

    private final List<OutboxEventIdResolver<?>> resolvers;

    public OutboxEventIdResolverManager(List<OutboxEventIdResolver<?>> resolvers) {
        this.resolvers = Collections.unmodifiableList(resolvers);
    }

    @Override
    public UUID resolve(T rowMessage) {
        for (OutboxEventIdResolver<?> resolver : resolvers) {
            if (resolver.supports(rowMessage.getClass())) {
                return ((OutboxEventIdResolver<T>) resolver).resolve(rowMessage);
            }
        }
        throw new IllegalArgumentException("Unsupported class '%s'; cannot resolve".formatted(rowMessage.getClass().getName()));
    }

    @Override
    public boolean supports(Class<?> c) {
        return Object.class.isAssignableFrom(c);
    }
}
