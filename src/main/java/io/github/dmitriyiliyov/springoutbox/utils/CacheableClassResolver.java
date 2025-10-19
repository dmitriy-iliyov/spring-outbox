package io.github.dmitriyiliyov.springoutbox.utils;

import java.util.HashMap;
import java.util.Map;

public final class CacheableClassResolver {

    private final Map<String, Class<?>> cachedClasses;

    public CacheableClassResolver() {
        this.cachedClasses = new HashMap<>();
    }

    public Class<?> resolve(String className) {
        return cachedClasses.computeIfAbsent(
                className,
                c -> {
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
}