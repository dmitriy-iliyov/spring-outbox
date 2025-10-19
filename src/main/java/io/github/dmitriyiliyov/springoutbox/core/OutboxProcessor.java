package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;

public interface OutboxProcessor {
    void process(OutboxProperties.EventProperties properties);
}
