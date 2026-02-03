package io.github.dmitriyiliyov.springoutbox.web;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.projection.BatchUpdateRequestProjection;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record BatchUpdateRequest(
        @NotEmpty(message = "Id list cannot be empty or null")
        Set<UUID> ids,
        @NotNull(message = "Status cannot be null")
        DlqStatus status
) implements BatchUpdateRequestProjection { }
