package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.starter.consumer.OutboxConsumerProperties;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import javax.sql.DataSource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OutboxAutoConfiguration.class);

    private final OutboxProperties properties;

    public OutboxAutoConfiguration(OutboxProperties properties) {
        this.properties = properties;
    }

    @Bean
    public OutboxPublisherProperties outboxPublishProperties() {
        return properties.getPublisher();
    }

    @Bean
    public OutboxConsumerProperties outboxConsumerProperties() {
        return properties.getConsumer();
    }

    @Bean
    @ConditionalOnProperty(prefix = "outbox.tables", name = "auto-create", havingValue = "true", matchIfMissing = true)
    public DataSourceInitializer outboxDataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDataSource(dataSource);
        dataSourceInitializer.setDatabasePopulator(OutboxDatabasePopulatorFactory.generate(properties, dataSource));
        return dataSourceInitializer;
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService outboxScheduledExecutorService() {
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("outbox-thread-");
        threadFactory.setDaemon(true);
        threadFactory.setThreadPriority(Thread.NORM_PRIORITY);
        return Executors.newScheduledThreadPool(
                properties.getThreadPoolSize(),
                threadFactory
        );
    }

//    @PreDestroy
//    public void onDestroy(@Qualifier("outboxScheduledExecutorService") ScheduledExecutorService executor) {
//        executor.shutdown();
//        try {
//            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
//                executor.shutdownNow();
//                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
//                    log.error("ScheduledExecutorService shutdown incorrectly");
//                }
//            }
//        } catch (InterruptedException ie) {
//            executor.shutdownNow();
//            Thread.currentThread().interrupt();
//        }
//    }

    @Bean
    @ConditionalOnMissingBean
    public PostApplicationStartOutboxInitializer outboxInitializer(ApplicationContext applicationContext) {
        return new PostApplicationStartOutboxInitializer(applicationContext);
    }
}
