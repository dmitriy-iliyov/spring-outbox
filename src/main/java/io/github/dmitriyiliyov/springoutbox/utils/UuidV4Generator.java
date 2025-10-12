package io.github.dmitriyiliyov.springoutbox.utils;

import java.util.UUID;

public class UuidV4Generator implements UuidGenerator {
    @Override
    public UUID generate() {
        return UUID.randomUUID();
    }
}
