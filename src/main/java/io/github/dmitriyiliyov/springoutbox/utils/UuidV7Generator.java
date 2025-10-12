package io.github.dmitriyiliyov.springoutbox.utils;

import java.util.UUID;

public class UuidV7Generator implements UuidGenerator {
    @Override
    public UUID generate() {
        throw new RuntimeException("UUID v7 generator");
    }
}
