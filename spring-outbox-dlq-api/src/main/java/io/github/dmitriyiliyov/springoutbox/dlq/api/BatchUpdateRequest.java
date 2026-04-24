package io.github.dmitriyiliyov.springoutbox.dlq.api;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

@Schema(description = "Batch update request for DLQ events")
public record BatchUpdateRequest(

        @Schema(
                description = "List of event ids to update",
                example = "[\"550e8400-e29b-41d4-a716-446655440000\", \"550e8400-e29b-41d4-a716-446655440001\"]",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maximum = "1000"
        )
        @NotEmpty(message = "Id list cannot be empty or null")
        @Size(max = 1000, message = "Maximum 1000 ids allowed")
        Set<UUID> ids,

        @Schema(
                description = "New status to assign to all specified events",
                example = "RESOLVED",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Status cannot be null")
        DlqStatus status

) { }
