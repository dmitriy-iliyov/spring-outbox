package io.github.dmitriyiliyov.oncebox.tests.e2e.domain;

import java.util.UUID;

public record BusinessEvent(
        UUID verifyId
) {
    public static BusinessEvent of() {
        return new BusinessEvent(UUID.randomUUID());
    }

    public static BusinessEvent of(UUID verifyId) {
        return new BusinessEvent(verifyId);
    }
}