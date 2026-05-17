package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MetricsPostApplicationReadyOutboxInitializer implements PostApplicationReadyOutboxInitializer {

    private static final Logger log = LoggerFactory.getLogger(MetricsPostApplicationReadyOutboxInitializer.class);

    private final Map<String, OutboxMetrics> metrics;

    public MetricsPostApplicationReadyOutboxInitializer(Map<String, OutboxMetrics> metrics) {
        this.metrics = metrics;
    }

    @Override
    public void init() {
        metrics.values().forEach(OutboxMetrics::register);
        log.debug("Outbox successfully initialized with metrics observers {}", metrics.keySet());
    }
}
