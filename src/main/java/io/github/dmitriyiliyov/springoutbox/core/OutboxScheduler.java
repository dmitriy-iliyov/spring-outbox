package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.core.config.OutboxProperties;

import java.util.concurrent.ScheduledExecutorService;

public final class OutboxScheduler {

    private final OutboxProperties properties;
    private final OutboxProcessor processor;
    private final OutboxSender sender;
    private final ScheduledExecutorService executor;


    public OutboxScheduler(OutboxProperties properties, OutboxProcessor processor, OutboxSender sender, ScheduledExecutorService executor) {
        this.properties = properties;
        this.processor = processor;
        this.sender = sender;
        this.executor = executor;
    }
}
