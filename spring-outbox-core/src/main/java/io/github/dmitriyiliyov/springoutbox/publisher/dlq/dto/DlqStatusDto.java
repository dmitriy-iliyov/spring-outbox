package io.github.dmitriyiliyov.springoutbox.publisher.dlq.dto;

import io.github.dmitriyiliyov.springoutbox.publisher.dlq.DlqStatus;
import jakarta.validation.constraints.NotNull;

public record DlqStatusDto(
        @NotNull(message = "Status cannot be null")
        DlqStatus status
) {}
