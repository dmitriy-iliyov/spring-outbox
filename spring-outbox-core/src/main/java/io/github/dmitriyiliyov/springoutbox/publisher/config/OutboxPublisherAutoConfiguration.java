package io.github.dmitriyiliyov.springoutbox.publisher.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.config.CleanUpProperties;
import io.github.dmitriyiliyov.springoutbox.publisher.core.*;
import io.github.dmitriyiliyov.springoutbox.publisher.core.aop.OutboxPublishAspect;
import io.github.dmitriyiliyov.springoutbox.publisher.core.aop.RowOutboxEventListener;
import io.github.dmitriyiliyov.springoutbox.publisher.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.publisher.metrics.DefaultOutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.publisher.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class OutboxPublisherAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherAutoConfiguration.class);

    private OutboxPublisherProperties properties;
    private ObjectMapper mapper;

    public OutboxPublisherAutoConfiguration(OutboxPublisherProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        log.debug("OutboxPublisherAutoConfiguration successfully created");
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
    public SmartInitializingSingleton outboxSchedulersInitializer(
            OutboxPublisherProperties outboxPublisherProperties,
            @Qualifier("outboxScheduledExecutorService") ScheduledExecutorService executor,
            OutboxProcessor processor,
            OutboxManager manager,
            ConfigurableListableBeanFactory factory
    ) {
        return () -> {
            log.debug("Start initialize schedulers beans");
            for (OutboxPublisherProperties.EventProperties event : outboxPublisherProperties.getEvents().values()) {
                String beanName = BeanNameUtils.toBeanName(event.getEventType(), "OutboxPublisherScheduler");
                if (!factory.containsBean(beanName)) {
                    factory.registerSingleton(beanName, new OutboxPublisherScheduler(event, executor, processor));
                    log.debug("Created bean with beanName {}", beanName);
                }
            }

            String recoverySchedulerBeanName = "outboxRecoveryScheduler";
            factory.registerSingleton(
                    recoverySchedulerBeanName,
                    new OutboxRecoveryScheduler(outboxPublisherProperties.getStuckRecovery(), executor, manager)
            );
            log.debug("Created bean with beanName {}", recoverySchedulerBeanName);

            if (outboxPublisherProperties.isCleanUpEnabled()) {
                CleanUpProperties cleanUpProperties = outboxPublisherProperties.getCleanUp();
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
    public OutboxPublishAspect outboxEventAspect(ApplicationEventPublisher eventPublisher) {
        return new OutboxPublishAspect(eventPublisher);
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
}
