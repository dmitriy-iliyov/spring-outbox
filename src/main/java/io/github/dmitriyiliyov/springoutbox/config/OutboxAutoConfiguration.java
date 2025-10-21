package io.github.dmitriyiliyov.springoutbox.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.*;
import io.github.dmitriyiliyov.springoutbox.core.aop.RowOutboxEventListener;
import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.metrics.DefaultOutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.utils.BeanNameUtils;
import io.github.dmitriyiliyov.springoutbox.utils.UuidV7Generator;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxAutoConfiguration implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(OutboxAutoConfiguration.class);
    private final OutboxProperties properties;
    private final ObjectMapper mapper;
    private ApplicationContext applicationContext;

    public OutboxAutoConfiguration(OutboxProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    public OutboxRepository outboxRepository(DataSource dataSource) {
        return OutboxRepositoryFactory.generate(dataSource);
    }

    @Bean
    public OutboxCache<EventStatus> outboxCache() {
        return new DumbOutboxCache<>(60, 30, 30);
    }

    @Bean
    public OutboxSerializer outboxSerializer() {
        return new JacksonOutboxSerializer(mapper, new UuidV7Generator());
    }

    @Bean
    public OutboxPublisher outboxPublisher(OutboxSerializer serializer, OutboxRepository repository) {
        return new DefaultOutboxPublisher(properties, serializer, repository);
    }

    @Bean
    public OutboxManager outboxManager(OutboxRepository repository, OutboxCache<EventStatus> cache) {
        return new DefaultOutboxManager(repository, cache);
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService outboxScheduledExecutorService() {
        return Executors.newScheduledThreadPool(properties.getThreadPoolSize());
    }

    @Override
    public void postProcessBeanDefinitionRegistry(@Nonnull BeanDefinitionRegistry registry) throws BeansException { }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        defineSender(registry);
        defineProcessor(registry);
        ScheduledExecutorService executor =
                beanFactory.getBean("outboxScheduledExecutorService", ScheduledExecutorService.class);
        defineEventSchedulers(registry, executor);
        defineCleanUpScheduler(registry, executor);
    }

    private void defineSender(BeanDefinitionRegistry registry) {
        if (!registry.containsBeanDefinition("outboxSender")) {
            BeanDefinition senderBean = BeanDefinitionBuilder
                    .genericBeanDefinition(
                            OutboxSender.class,
                            () -> OutboxSenderFactory.generate(properties.getSender(), applicationContext, mapper)
                    )
                    .getBeanDefinition();
            registry.registerBeanDefinition("outboxSender", senderBean);
        }
    }

    private void defineProcessor(BeanDefinitionRegistry registry) {
        if (!registry.containsBeanDefinition("outboxProcessor")) {
            BeanDefinition processorBean = BeanDefinitionBuilder
                    .genericBeanDefinition(
                            OutboxProcessor.class,
                            () -> new DefaultOutboxProcessor(
                                    (OutboxManager) applicationContext.getBean("outboxManager"),
                                    (OutboxSender) applicationContext.getBean("outboxSender")
                            )
                    )
                    .getBeanDefinition();
            registry.registerBeanDefinition("outboxProcessor", processorBean);
        }
    }

    private void defineEventSchedulers(BeanDefinitionRegistry registry, ScheduledExecutorService executor) {
        for (OutboxProperties.EventProperties eventProperties : properties.getEvents().values()) {
            String beanName = BeanNameUtils.toBeanName(eventProperties.eventType(), "OutboxScheduler");
            if (!registry.containsBeanDefinition(beanName)) {
                BeanDefinition schedulerBean = BeanDefinitionBuilder
                        .genericBeanDefinition(OutboxEventScheduler.class)
                        .addConstructorArgValue(eventProperties)
                        .addConstructorArgValue(executor)
                        .addConstructorArgReference("outboxProcessor")
                        .getBeanDefinition();
                registry.registerBeanDefinition(beanName, schedulerBean);
            } else {
                log.warn("Scheduler bean {} already exists, skipping registration", beanName);
            }
        }
    }

    private void defineCleanUpScheduler(BeanDefinitionRegistry registry, ScheduledExecutorService executor) {
        OutboxProperties.CleanUpProperties cleanUpProperties = properties.getCleanUp();
        if (cleanUpProperties.enabled()) {
            BeanDefinition cleanUpBean = BeanDefinitionBuilder
                    .genericBeanDefinition(OutboxCleanUpScheduler.class)
                    .addConstructorArgValue(executor)
                    .addConstructorArgValue(cleanUpProperties)
                    .addConstructorArgReference("outboxManager")
                    .getBeanDefinition();
            registry.registerBeanDefinition("defaultOutboxCleanUpScheduler", cleanUpBean);
        } else {
            log.warn("Outbox is configured without a cleanup scheduler bean because clean-up is disabled; " +
                    "outbox storage will not be cleaned automatically.");
        }
    }

    @Bean
    public RowOutboxEventListener rowOutboxEventListener(OutboxPublisher publisher) {
        return new RowOutboxEventListener(publisher);
    }

    @Bean
    public OutboxMetrics defaultOutboxMetrics(MeterRegistry registry, OutboxManager manager) {
        return new DefaultOutboxMetrics(registry, properties, manager);
    }

    @Bean
    public OutboxStarter start(List<OutboxScheduler> schedulers, List<OutboxMetrics> metrics) {
        return new OutboxStarter(schedulers, metrics);
    }
}
