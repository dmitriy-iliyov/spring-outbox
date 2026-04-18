package io.github.dmitriyiliyov.springoutbox.web;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "DLQ status container")
public record DlqStatusDto(

        @Schema(
                description = "DLQ status to set as new event status",
                example = "RESOLVED",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Status cannot be null")
        DlqStatus status
) {}
