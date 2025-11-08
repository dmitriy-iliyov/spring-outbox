package io.github.dmitriyiliyov.springoutbox.publisher.core;

import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties;

public interface OutboxProcessor {
    void process(OutboxPublisherProperties.EventProperties properties);
}
