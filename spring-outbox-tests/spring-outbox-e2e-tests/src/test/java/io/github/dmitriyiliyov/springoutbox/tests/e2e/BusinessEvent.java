package io.github.dmitriyiliyov.springoutbox.tests.e2e;

import java.util.UUID;

public record BusinessEvent(
        UUID verifyId
) {
    public static BusinessEvent of() {
        return new BusinessEvent(UUID.randomUUID());
    }
}
