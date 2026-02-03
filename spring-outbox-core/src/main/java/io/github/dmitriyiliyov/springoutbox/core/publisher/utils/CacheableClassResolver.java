package io.github.dmitriyiliyov.springoutbox.core.publisher.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CacheableClassResolver {

    private final Map<String, Class<?>> cachedClasses = new ConcurrentHashMap<>();

    public Class<?> resolve(String className) {
        return cachedClasses.computeIfAbsent(
                className,
                c -> {
                    try {
                        return Class.forName(c);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
}