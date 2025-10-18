package io.github.dmitriyiliyov.springoutbox.core.domain;

import java.util.Set;
import java.util.UUID;

public record SenderResult(
        Set<UUID> processedIds,
        Set<UUID> failedIds
) { }
