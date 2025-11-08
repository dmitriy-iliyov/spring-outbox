package io.github.dmitriyiliyov.springoutbox.publisher.dlq.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;
import java.util.UUID;

public record DeleteBatchRequest(
        @NotEmpty(message = "Id list cannot be empty or null")
        Set<UUID> ids
) { }
