package io.github.dmitriyiliyov.springoutbox.publisher.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class RepositoryUtils {

    private static final Logger log = LoggerFactory.getLogger(RepositoryUtils.class);

    public static boolean validateIds(Set<UUID> ids) {
        Objects.requireNonNull(ids, "ids cannot be null");
        if (ids.isEmpty()) {
            return false;
        }
        if (ids.size() > 100) {
            log.warn("Batch size {} is too large...", ids.size());
            return false;
        }
        return true;
    }

    public static String generatePlaceholders(Set<UUID> ids) {
        return ids.stream().map(id -> "?").collect(Collectors.joining(", "));
    }
}
