package io.github.dmitriyiliyov.springoutbox.dlq.dto;

import io.github.dmitriyiliyov.springoutbox.dlq.DlqStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record BatchUpdateRequest(
        @NotEmpty(message = "Id list cannot be empty or null")
        Set<UUID> ids,
        @NotNull(message = "Status cannot be null")
        DlqStatus status
) { }
