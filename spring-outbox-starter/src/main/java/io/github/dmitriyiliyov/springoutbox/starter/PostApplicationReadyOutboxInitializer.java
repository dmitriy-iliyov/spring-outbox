package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.Map;

public class PostApplicationReadyOutboxInitializer {

    private static final Logger log = LoggerFactory.getLogger(PostApplicationReadyOutboxInitializer.class);

    private final OutboxProperties properties;
    private final Map<String, OutboxScheduler> schedulers;
    private final Map<String, OutboxMetrics> metrics;

    public PostApplicationReadyOutboxInitializer(OutboxProperties properties, Map<String, OutboxScheduler> schedulers, Map<String, OutboxMetrics> metrics) {
        this.properties = properties;
        this.schedulers = schedulers;
        this.metrics = metrics;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        schedulers.values().forEach(OutboxScheduler::schedule);
        log.debug("Outbox successfully initialized with schedulers {}", schedulers.keySet());
        metrics.values().forEach(OutboxMetrics::register);
        log.debug("Outbox successfully initialized with metrics observers {}", metrics.keySet());
        log.debug(LogUtils.prettyPrint(properties));
    }
}
