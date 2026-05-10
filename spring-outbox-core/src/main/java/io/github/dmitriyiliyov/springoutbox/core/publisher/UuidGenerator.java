package io.github.dmitriyiliyov.springoutbox.core.publisher;

import java.util.UUID;

/**
 * Abstraction for generating outbox event id
 */
public interface UuidGenerator {
    UUID generate();
}
