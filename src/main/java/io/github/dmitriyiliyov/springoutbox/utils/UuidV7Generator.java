package io.github.dmitriyiliyov.springoutbox.utils;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

public class UuidV7Generator implements UuidGenerator {
    @Override
    public UUID generate() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
