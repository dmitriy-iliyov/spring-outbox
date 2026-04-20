package io.github.dmitriyiliyov.springoutbox.web;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Batch get request for DLQ events")
public record BatchRequest(

        @Schema(
                description = "Status to filter events",
                example = "MOVED",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Status cannot be null")
        DlqStatus status,

        @Schema(description = "Zero-based batch index (pagination offset in batches)", example = "1")
        @PositiveOrZero(message = "Batch number cannot be negative")
        int batchNumber,

        @Schema(
                description = "Number of events per batch",
                example = "50",
                minimum = "10",
                maximum = "100"
        )
        @Min(value = 10, message = "Min batch size is 10")
        @Max(value = 100, message = "Max batch size is 100")
        int batchSize
) { }
