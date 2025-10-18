package io.github.dmitriyiliyov.springoutbox.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.aop.OutboxEventAspect;
import io.github.dmitriyiliyov.springoutbox.aop.RowOutboxEventListener;
import io.github.dmitriyiliyov.springoutbox.core.*;
import io.github.dmitriyiliyov.springoutbox.utils.BeanNameUtils;
import io.github.dmitriyiliyov.springoutbox.utils.UuidV7Generator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxAutoConfiguration implements BeanDefinitionRegistryPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxAutoConfiguration.class);
    private final OutboxProperties properties;
    private final ObjectMapper mapper;

    public OutboxAutoConfiguration(OutboxProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
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

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService outboxScheduledExecutorService() {
        return Executors.newScheduledThreadPool(properties.getThreadPoolSize());
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException { }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        DefaultOutboxProcessor processor = beanFactory.getBean(DefaultOutboxProcessor.class);
        OutboxSender sender = new SyncKafkaOutboxSender(null, mapper);
        ScheduledExecutorService executor = beanFactory.getBean("outboxScheduledExecutorService", ScheduledExecutorService.class);
        for (OutboxProperties.EventProperties eventProperties : properties.getEvents().values()) {
            BeanDefinition schedulerBean = BeanDefinitionBuilder
                    .genericBeanDefinition(OutboxEventScheduler.class)
                    .addConstructorArgValue(eventProperties)
                    .addConstructorArgValue(processor)
                    .addConstructorArgValue(sender)
                    .addConstructorArgValue(executor)
                    .getBeanDefinition();
            String beanName = BeanNameUtils.toBeanName(eventProperties.eventType(), "OutboxScheduler");
            if (!((BeanDefinitionRegistry) beanFactory).containsBeanDefinition(beanName)) {
                ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition(beanName, schedulerBean);
            } else {
                log.warn("Scheduler bean {} already exists, skipping registration", beanName);
            }
            ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition(beanName, schedulerBean);
        }
        OutboxProperties.CleanUpProperties cleanUpProperties = properties.getCleanUp();
        if (cleanUpProperties.isEnabled()) {
            BeanDefinition schedulerBean = BeanDefinitionBuilder
                    .genericBeanDefinition(OutboxCleanUpScheduler.class)
                    .addConstructorArgValue(cleanUpProperties)
                    .addConstructorArgValue(processor)
                    .addConstructorArgValue(executor)
                    .getBeanDefinition();
            ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition("defaultOutboxCleanUpScheduler", schedulerBean);
        } else {
            log.warn("Outbox is configured without a cleanup scheduler bean because clean-up is disabled; " +
                     "outbox storage will not be cleaned automatically.");
        }
    }

    @Bean
    public RowOutboxEventListener rowOutboxEventListener(OutboxPublisher publisher) {
        return new RowOutboxEventListener(publisher);
    }
}
