package io.github.dmitriyiliyov.springoutbox.web;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import jakarta.validation.constraints.NotNull;

public record DlqStatusDto(
        @NotNull(message = "Status cannot be null")
        DlqStatus status
) {}
