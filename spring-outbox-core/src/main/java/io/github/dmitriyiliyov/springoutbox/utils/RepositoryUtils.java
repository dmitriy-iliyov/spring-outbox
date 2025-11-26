package io.github.dmitriyiliyov.springoutbox.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public static String generateValuesPlaceholders(Set<UUID> ids, int valueCount) {
        return ids.stream()
                .map(id -> {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < valueCount; i++) {
                        if (i < valueCount - 1) {
                            sb.append("?, ");
                        } else {
                            sb.append("?");
                        }
                    }
                    return "(%s)".formatted(sb);
                })
                .collect(Collectors.joining(", "));
    }
}
