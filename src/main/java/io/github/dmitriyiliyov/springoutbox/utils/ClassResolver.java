package io.github.dmitriyiliyov.springoutbox.utils;

import java.util.HashMap;
import java.util.Map;

public final class ClassResolver {

    private final Map<String, Class<?>> classes;

    public ClassResolver() {
        this.classes = new HashMap<>();
    }

    public Class<?> resolve(String className) {
        return classes.computeIfAbsent(
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