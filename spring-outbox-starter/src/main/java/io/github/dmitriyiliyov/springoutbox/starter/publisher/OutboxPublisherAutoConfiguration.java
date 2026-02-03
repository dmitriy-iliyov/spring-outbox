package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.aop.OutboxPublishAspect;
import io.github.dmitriyiliyov.springoutbox.aop.RowOutboxEventListener;
import io.github.dmitriyiliyov.springoutbox.core.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.core.publisher.*;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.metrics.DefaultOutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.core.publisher.metrics.OutboxManagerMetricsDecorator;
import io.github.dmitriyiliyov.springoutbox.core.publisher.utils.*;
import io.github.dmitriyiliyov.springoutbox.starter.BeanNameUtils;
import io.github.dmitriyiliyov.springoutbox.starter.OutboxProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ConditionalOnProperty(prefix = "outbox.publisher", name = "enabled", havingValue = "true", matchIfMissing = true)
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
    @ConditionalOnMissingBean
    public OutboxRepository outboxRepository(DataSource dataSource) {
        return OutboxRepositoryFactory.generate(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxCache<EventStatus> outboxCache() {
        OutboxPublisherProperties.MetricsProperties metricsProperties = properties.getMetrics();
        if (metricsProperties != null) {
            OutboxPublisherProperties.MetricsProperties.GaugeProperties gaugeProperties = metricsProperties.getGauge();
            if (gaugeProperties != null) {
                if (gaugeProperties.isEnabled()) {
                    List<Duration> ttls = gaugeProperties.getCache().getTtls();
                    if (ttls == null || ttls.isEmpty()) {
                        throw new IllegalArgumentException("Cache ttls cannot be null or empty");
                    }
                    if (ttls.size() != 3) {
                        throw new IllegalArgumentException("Ttls should be 3 element size");
                    }
                    return new SimpleOutboxCache<>(
                            ttls.get(0).toSeconds(), ttls.get(1).toSeconds(), ttls.get(2).toSeconds()
                    );
                }
            }
        }
        return new PassthroughOutboxCache<>();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxManager outboxManager(OutboxRepository repository,
                                       OutboxCache<EventStatus> cache,
                                       MeterRegistry registry) {
        return new OutboxManagerMetricsDecorator(new DefaultOutboxManager(repository, cache), properties, registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxSender outboxSender(ApplicationContext context) {
        return OutboxSenderFactory.generate(properties.getSender(), context, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
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
            for (OutboxPublisherProperties.EventPropertiesHolder event : outboxPublisherProperties.getEvents().values()) {
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
                OutboxProperties.CleanUpProperties cleanUpProperties = outboxPublisherProperties.getCleanUp();
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
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetrics outboxMetrics(MeterRegistry registry, OutboxManager manager) {
        return new DefaultOutboxMetrics(registry, properties, manager);
    }
}
