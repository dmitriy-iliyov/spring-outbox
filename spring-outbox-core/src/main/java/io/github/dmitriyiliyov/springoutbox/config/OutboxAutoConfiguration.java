package io.github.dmitriyiliyov.springoutbox.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.*;
import io.github.dmitriyiliyov.springoutbox.core.aop.OutboxEventAspect;
import io.github.dmitriyiliyov.springoutbox.core.aop.RowOutboxEventListener;
import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.metrics.DefaultOutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.utils.*;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OutboxAutoConfiguration.class);

    private OutboxProperties properties;
    private ObjectMapper mapper;

    public OutboxAutoConfiguration(OutboxProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        log.debug("OutboxAutoConfiguration successfully created");
    }

    @Bean
    public OutboxRepository outboxRepository(DataSource dataSource) {
        return OutboxRepositoryFactory.generate(dataSource);
    }

    @Bean
    public OutboxCache<EventStatus> outboxCache() {
        return new SimpleOutboxCache<>(60, 30, 30);
    }

    @Bean
    public OutboxManager outboxManager(OutboxRepository repository, OutboxCache<EventStatus> cache) {
        return new DefaultOutboxManager(repository, cache);
    }

    @Bean
    public OutboxSender outboxSender(ApplicationContext context) {
        return OutboxSenderFactory.generate(properties.getSender(), context, mapper);
    }

    @Bean
    public OutboxProcessor outboxProcessor(OutboxManager manager, OutboxSender sender) {
        return new DefaultOutboxProcessor(manager, sender);
    }

    @Bean
    public ScheduledExecutorService outboxScheduledExecutorService() {
        return Executors.newScheduledThreadPool(properties.getThreadPoolSize());
    }

    @PreDestroy
    public void onDestroy() {
        ScheduledExecutorService executor = outboxScheduledExecutorService();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.error("ExecutorService shutdown incorrectly");
                }
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Bean
    public static BeanFactoryPostProcessor outboxDynamicSchedulers(OutboxProperties outboxProperties) {
        return factory -> {
            ScheduledExecutorService executor = factory.getBean("outboxScheduledExecutorService", ScheduledExecutorService.class);
            OutboxProcessor processor = factory.getBean(OutboxProcessor.class);
            for (OutboxProperties.EventProperties event : outboxProperties.getEvents().values()) {
                String name = BeanNameUtils.toBeanName(event.getEventType(), "OutboxScheduler");
                if (!factory.containsBean(name)) {
                    factory.registerSingleton(name, new OutboxPublisherScheduler(event, executor, processor));
                }
            }
            OutboxManager manager = factory.getBean(OutboxManager.class);
            factory.registerSingleton("outboxRecoveryScheduler",
                    new OutboxRecoveryScheduler(outboxProperties.getStuckRecovery(), executor, manager)
            );
            if (outboxProperties.isCleanUpEnabled()) {
                OutboxProperties.CleanUpProperties cleanUpProperties = outboxProperties.getCleanUp();
                if (cleanUpProperties == null) {
                    throw new IllegalStateException("OutboxProperties.CleanUpProperties is null for some reason");
                }
                factory.registerSingleton(
                        "outboxCleanUpScheduler",
                        new OutboxCleanUpScheduler(cleanUpProperties, executor, manager)
                );
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public UuidGenerator outboxUuidGenerator() {
        return new UuidV7Generator();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxSerializer outboxSerializer(UuidGenerator uuidGenerator) {
        return new JacksonOutboxSerializer(mapper, uuidGenerator);
    }

    @Bean
    public OutboxPublisher outboxPublisher(OutboxSerializer serializer, OutboxManager manager) {
        return new DefaultOutboxPublisher(properties, serializer, manager);
    }

    @Bean
    public OutboxEventAspect outboxEventAspect(ApplicationEventPublisher eventPublisher) {
        return new OutboxEventAspect(eventPublisher);
    }

    @Bean
    public RowOutboxEventListener rowOutboxEventListener(OutboxPublisher publisher) {
        return new RowOutboxEventListener(publisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxMetrics outboxMetrics(MeterRegistry registry, OutboxManager manager) {
        return new DefaultOutboxMetrics(registry, properties, manager);
    }

    @Bean
    public OutboxInitializer outboxInitializer(List<OutboxScheduler> schedulers, List<OutboxMetrics> metrics) {
        return new OutboxInitializer(schedulers, metrics);
    }
}
