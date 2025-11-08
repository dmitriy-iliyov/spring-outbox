package io.github.dmitriyiliyov.springoutbox.publisher.core.domain;

import java.util.Set;
import java.util.UUID;

public record SenderResult(
        Set<UUID> processedIds,
        Set<UUID> failedIds
) {
    public static SenderResult empty() {
        return new SenderResult(null, null);
    }
}
