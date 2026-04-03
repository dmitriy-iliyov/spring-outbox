package io.github.dmitriyiliyov.springoutbox.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;

import java.util.Map;

public class PostApplicationReadyOutboxInitializer {

    private static final Logger log = LoggerFactory.getLogger(PostApplicationReadyOutboxInitializer.class);

    private final ApplicationContext applicationContext;

    public PostApplicationReadyOutboxInitializer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        Map<String, OutboxScheduler> schedulersMap = applicationContext.getBeansOfType(OutboxScheduler.class);
        schedulersMap.values().forEach(OutboxScheduler::schedule);
        log.debug("Outbox successfully initialized with schedulers {}", schedulersMap.keySet());

        Map<String, OutboxMetrics> metricsMap = applicationContext.getBeansOfType(OutboxMetrics.class);
        metricsMap.values().forEach(OutboxMetrics::register);
        log.debug("Outbox successfully initialized with metrics observers {}", metricsMap.keySet());

        Map<String, OutboxProperties> propertiesMap = applicationContext.getBeansOfType(OutboxProperties.class);
        Map<String, ObjectMapper> objectMappers = applicationContext.getBeansOfType(ObjectMapper.class);
        if (propertiesMap.isEmpty()) {
            throw new IllegalStateException("No OutboxProperties.class in application context");
        }
        OutboxProperties properties = propertiesMap.values().stream().findFirst().get();
        if (!objectMappers.isEmpty()) {
            try {
                ObjectMapper mapper = objectMappers.values().stream().findFirst().get();
                log.debug(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(properties));
            } catch (Exception e) {
                log.error("Error when try log outbox properties", e);
            }
        } else {
            log.debug(properties.toString());
        }
    }
}
