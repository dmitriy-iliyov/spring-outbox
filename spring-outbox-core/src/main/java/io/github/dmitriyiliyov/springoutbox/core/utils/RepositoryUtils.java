package io.github.dmitriyiliyov.springoutbox.core.utils;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class RepositoryUtils {

    private RepositoryUtils() {}

    public static boolean isIdsValid(Set<UUID> ids) {
        Objects.requireNonNull(ids, "ids cannot be null");
        if (ids.isEmpty()) {
            return false;
        }
        return true;
    }

    public static String generateIdsPlaceholders(Set<UUID> ids) {
        return ids.stream().map(id -> "?").collect(Collectors.joining(", "));
    }

    public static String generateValuesPlaceholders(int tupleCount, int valueCount) {
        return IntStream.range(0, tupleCount)
                .mapToObj(i -> "(" + "?,".repeat(valueCount - 1) + "?" + ")")
                .collect(Collectors.joining(", "));
    }
}
