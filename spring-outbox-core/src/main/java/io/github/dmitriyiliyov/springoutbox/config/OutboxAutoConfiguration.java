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
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import javax.sql.DataSource;
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
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("outbox-thread-");
        threadFactory.setDaemon(true);
        threadFactory.setThreadPriority(Thread.NORM_PRIORITY);
        return Executors.newScheduledThreadPool(
                properties.getThreadPoolSize(),
                threadFactory
        );
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
    public SmartInitializingSingleton outboxDynamicSchedulersInitializer(
            OutboxProperties outboxProperties,
            @Qualifier("outboxScheduledExecutorService") ScheduledExecutorService executor,
            OutboxProcessor processor,
            OutboxManager manager,
            ConfigurableListableBeanFactory factory
    ) {
        return () -> {
            log.debug("Start initialize schedulers beans");
            for (OutboxProperties.EventProperties event : outboxProperties.getEvents().values()) {
                String beanName = BeanNameUtils.toBeanName(event.getEventType(), "OutboxPublisherScheduler");
                if (!factory.containsBean(beanName)) {
                    factory.registerSingleton(beanName, new OutboxPublisherScheduler(event, executor, processor));
                    log.debug("Created bean with beanName {}", beanName);
                }
            }

            String recoverySchedulerBeanName = "outboxRecoveryScheduler";
            factory.registerSingleton(
                    recoverySchedulerBeanName,
                    new OutboxRecoveryScheduler(outboxProperties.getStuckRecovery(), executor, manager)
            );
            log.debug("Created bean with beanName {}", recoverySchedulerBeanName);

            if (outboxProperties.isCleanUpEnabled()) {
                OutboxProperties.CleanUpProperties cleanUpProperties = outboxProperties.getCleanUp();
                if (cleanUpProperties == null) {
                    throw new IllegalStateException("OutboxProperties.CleanUpProperties is null");
                }
                String cleanUpSchedulerBeanName = "outboxCleanUpScheduler";
                factory.registerSingleton(
                        cleanUpSchedulerBeanName,
                        new OutboxCleanUpScheduler(cleanUpProperties, executor, manager)
                );
                log.debug("Created bean with beanName {}", cleanUpSchedulerBeanName);
            }
            log.debug("Schedulers beans successfully initialized");
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
    public OutboxInitializer outboxInitializer(ApplicationContext applicationContext) {
        return new OutboxInitializer(applicationContext);
    }
}
