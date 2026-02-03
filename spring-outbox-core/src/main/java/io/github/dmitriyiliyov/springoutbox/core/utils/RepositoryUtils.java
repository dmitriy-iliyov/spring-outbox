package io.github.dmitriyiliyov.springoutbox.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class RepositoryUtils {

    private static final Logger log = LoggerFactory.getLogger(RepositoryUtils.class);

    public static boolean isIdsValid(Set<UUID> ids) {
        Objects.requireNonNull(ids, "ids cannot be null");
        if (ids.isEmpty()) {
            return false;
        }
        if (ids.size() > 1000) {
            log.warn("Batch size {} is too large...", ids.size());
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
