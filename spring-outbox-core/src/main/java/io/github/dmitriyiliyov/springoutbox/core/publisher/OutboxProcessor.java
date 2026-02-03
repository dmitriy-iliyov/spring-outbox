package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;

public interface OutboxProcessor {
    void process(OutboxPublisherPropertiesHolder.EventPropertiesHolder properties);
}
