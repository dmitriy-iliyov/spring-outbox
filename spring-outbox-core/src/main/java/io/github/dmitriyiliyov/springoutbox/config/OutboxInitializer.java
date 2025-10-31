package io.github.dmitriyiliyov.springoutbox.config;

import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.List;

public final class OutboxInitializer {

    private final List<OutboxScheduler> schedulers;
    private final List<OutboxMetrics> metrics;

    public OutboxInitializer(List<OutboxScheduler> schedulers, List<OutboxMetrics> metrics) {
        this.schedulers = schedulers;
        this.metrics = metrics;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        schedulers.forEach(OutboxScheduler::schedule);
        metrics.forEach(OutboxMetrics::register);
    }
}
