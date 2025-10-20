package io.github.dmitriyiliyov.springoutbox.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.aop.RowOutboxEventListener;
import io.github.dmitriyiliyov.springoutbox.core.*;
import io.github.dmitriyiliyov.springoutbox.utils.BeanNameUtils;
import io.github.dmitriyiliyov.springoutbox.utils.UuidV7Generator;
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
        this.mapper = mapper;
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
    public OutboxSerializer outboxSerializer() {
        return new JacksonOutboxSerializer(mapper, new UuidV7Generator());
    }

    @Bean
    public OutboxPublisher outboxPublisher(OutboxSerializer serializer, OutboxRepository repository) {
        return new DefaultOutboxPublisher(properties, serializer, repository);
    }

    @Bean
    public OutboxManager outboxManager(OutboxRepository repository) {
        return new DefaultOutboxManager(repository);
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService outboxScheduledExecutorService() {
        return Executors.newScheduledThreadPool(properties.getThreadPoolSize());
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException { }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        defineSender(registry);
        defineProcessor(registry);
        ScheduledExecutorService executor =
                beanFactory.getBean("outboxScheduledExecutorService", ScheduledExecutorService.class);
        defineEventSchedulers(registry, executor);
        defineCleanUpScheduler(registry, executor);
        defineDlqComponents(registry, executor);
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

    private void defineDlqComponents(BeanDefinitionRegistry registry, ScheduledExecutorService executor) {
        OutboxProperties.DlqProperties dlqProperties = properties.getDlq();
        if (dlqProperties.enabled()) {

        } else {
            log.warn("Outbox is configured with DLQ disabled; no dead letter queue beans will be registered. " +
                    "Failed events will not be automatically cleaned or moved to DLQ.");
        }
    }

    private void defineDlqManager() {

    }

    private void defineDlqScheduler() {

    }

    @Bean
    public RowOutboxEventListener rowOutboxEventListener(OutboxPublisher publisher) {
        return new RowOutboxEventListener(publisher);
    }
}
