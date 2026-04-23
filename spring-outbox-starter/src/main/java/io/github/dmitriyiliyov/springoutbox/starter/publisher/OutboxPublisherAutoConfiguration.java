package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.aop.OutboxPublishAspect;
import io.github.dmitriyiliyov.springoutbox.aop.RowOutboxEventListener;
import io.github.dmitriyiliyov.springoutbox.core.publisher.*;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.utils.UuidGenerator;
import io.github.dmitriyiliyov.springoutbox.core.publisher.utils.UuidV7Generator;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.*;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.OutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.springoutbox.starter.BeanNameUtils;
import io.github.dmitriyiliyov.springoutbox.starter.OutboxProperties;
import io.github.dmitriyiliyov.springoutbox.starter.OutboxScheduleStrategyFactory;
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
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Clock;
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
    public OutboxRepository outboxRepository(
            DataSource dataSource,
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
            Clock clock
    ) {
        return OutboxRepositoryFactory.create(dataSource, jdbcTemplate, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxCache<EventStatus> outboxCache() {
        OutboxProperties.MetricsProperties metricsProperties = properties.getMetrics();
        if (metricsProperties != null) {
            OutboxProperties.MetricsProperties.GaugeProperties gaugeProperties = metricsProperties.getGauge();
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
        return new NoopOutboxCache<>();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxManager defaultOutboxManager(OutboxRepository repository, Clock clock) {
        return new DefaultOutboxManager(repository, clock);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxManager outboxManagerMetricsDecorator(OutboxManager manager, MeterRegistry registry) {
        return new OutboxManagerMetricsDecorator(properties, registry, manager);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxSender outboxSender(ApplicationContext context) {
        return OutboxSenderFactory.create(properties.getSender(), context, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxProcessor outboxProcessor(OutboxManager manager, OutboxSender sender, Clock clock) {
        return new DefaultOutboxProcessor(manager, sender, clock);
    }

    @Bean
    public SmartInitializingSingleton outboxSchedulersInitializer(
            OutboxPublisherProperties publisherProperties,
            @Qualifier("outboxScheduledExecutorService") ScheduledExecutorService executor,
            OutboxProcessor processor,
            OutboxManager manager,
            Clock clock,
            ConfigurableListableBeanFactory factory
    ) {
        return () -> {
            log.debug("Start initialize schedulers beans");
            log.debug(manager.getClass().getName());
            for (OutboxPublisherProperties.EventProperties event : publisherProperties.getEvents().values()) {
                String beanName = BeanNameUtils.toBeanName(event.getEventType(), "OutboxPublisherScheduler");
                if (!factory.containsBean(beanName)) {
                    factory.registerSingleton(
                            beanName,
                            new OutboxPollingScheduler(
                                    event,
                                    OutboxScheduleStrategyFactory.create(event.getPolling(), executor),
                                    processor
                            )
                    );
                    log.debug("Created bean with beanName {}", beanName);
                }
            }

            String recoverySchedulerBeanName = "outboxRecoveryScheduler";
            factory.registerSingleton(
                    recoverySchedulerBeanName,
                    new OutboxRecoveryScheduler(
                            publisherProperties.getStuckRecovery(),
                            OutboxScheduleStrategyFactory.create(
                                    publisherProperties.getStuckRecovery().getPolling(),
                                    executor
                            ),
                            manager
                    )
            );
            log.debug("Created bean with beanName {}", recoverySchedulerBeanName);

            if (publisherProperties.isCleanUpEnabled()) {
                OutboxProperties.CleanUpProperties cleanUpProperties = publisherProperties.getCleanUp();
                if (cleanUpProperties == null) {
                    throw new IllegalStateException("OutboxProperties.CleanUpProperties is null");
                }
                String cleanUpSchedulerBeanName = "outboxCleanUpScheduler";
                factory.registerSingleton(
                        cleanUpSchedulerBeanName,
                        new OutboxCleanUpScheduler(
                                cleanUpProperties,
                                OutboxScheduleStrategyFactory.create(cleanUpProperties.getPolling(), executor),
                                clock,
                                manager
                        )
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
    public OutboxSerializer outboxSerializer(UuidGenerator uuidGenerator, Clock clock) {
        return new JacksonOutboxSerializer(mapper, uuidGenerator, clock);
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
            prefix = "outbox.publisher.metrics",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetricsRepository outboxMetricsRepository(
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new MultiDialectOutboxMetricsRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetricsService outboxMetricsService(OutboxMetricsRepository repository, OutboxCache<EventStatus> cache) {
        return new DefaultOutboxMetricsService(repository, cache);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetrics outboxMetrics(MeterRegistry registry, OutboxMetricsService metricsService) {
        return new DefaultOutboxMetrics(properties, registry, metricsService);
    }
}
