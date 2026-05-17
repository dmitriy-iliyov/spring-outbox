package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SchedulersPostApplicationReadyOutboxInitializer implements PostApplicationReadyOutboxInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchedulersPostApplicationReadyOutboxInitializer.class);

    private final Map<String, OutboxScheduler> schedulers;

    public SchedulersPostApplicationReadyOutboxInitializer(Map<String, OutboxScheduler> schedulers) {
        this.schedulers = schedulers;
    }

    @Override
    public void init() {
        schedulers.values().forEach(OutboxScheduler::schedule);
        log.debug("Outbox successfully initialized with schedulers {}", schedulers.keySet());
    }
}
