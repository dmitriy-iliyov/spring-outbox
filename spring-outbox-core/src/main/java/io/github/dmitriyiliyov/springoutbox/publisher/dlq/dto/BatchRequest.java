package io.github.dmitriyiliyov.springoutbox.publisher.dlq.dto;

import io.github.dmitriyiliyov.springoutbox.publisher.dlq.DlqStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record BatchRequest(
        @NotNull(message = "Status cannot be null")
        DlqStatus status,
        @PositiveOrZero(message = "Batch number cannot be negative")
        int batchNumber,
        @Min(value = 10, message = "Min batch size is 10")
        @Max(value = 100, message = "Max batch size is 100")
        int batchSize
) { }
