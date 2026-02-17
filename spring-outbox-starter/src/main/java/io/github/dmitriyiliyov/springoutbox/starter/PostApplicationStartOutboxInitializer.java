package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;

import java.util.Map;

public class PostApplicationStartOutboxInitializer {

    private static final Logger log = LoggerFactory.getLogger(PostApplicationStartOutboxInitializer.class);

    private final ApplicationContext applicationContext;

    public PostApplicationStartOutboxInitializer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        Map<String, OutboxScheduler> schedulersMap = applicationContext.getBeansOfType(OutboxScheduler.class);
        schedulersMap.values().forEach(OutboxScheduler::schedule);
        Map<String, OutboxMetrics> metricsMap = applicationContext.getBeansOfType(OutboxMetrics.class);
        metricsMap.values().forEach(OutboxMetrics::register);
        log.debug("Outbox successfully initialized with schedulers {}", schedulersMap.keySet());
    }
}
