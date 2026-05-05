package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.List;

public class PostApplicationReadyOutboxInitializer {

    private static final Logger log = LoggerFactory.getLogger(PostApplicationReadyOutboxInitializer.class);

    private final OutboxProperties properties;
    private final List<OutboxMetrics> metrics;
    private final List<OutboxScheduler> schedulers;

    public PostApplicationReadyOutboxInitializer(OutboxProperties properties, List<OutboxMetrics> metrics, List<OutboxScheduler> schedulers) {
        this.properties = properties;
        this.metrics = metrics;
        this.schedulers = schedulers;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        schedulers.forEach(OutboxScheduler::schedule);
        log.debug("Outbox successfully initialized with schedulers {}", schedulers);
        metrics.forEach(OutboxMetrics::register);
        log.debug("Outbox successfully initialized with metrics observers {}", metrics);
        log.debug(LogUtils.prettyPrint(properties));
    }
}
