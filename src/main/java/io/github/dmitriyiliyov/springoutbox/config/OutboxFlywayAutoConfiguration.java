package io.github.dmitriyiliyov.springoutbox.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(prefix = "outbox.migration", name = "enabled", havingValue = "true")
@ConditionalOnClass(Flyway.class)
public class OutboxFlywayAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OutboxFlywayAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(name = "outboxFlyway")
    public Flyway outboxFlyway(DataSource dataSource, OutboxProperties outboxProperties) {
        log.info("Initializing Outbox Flyway migrations");
        OutboxProperties.MigrationProperties properties = outboxProperties.getMigration();
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(properties.getLocation())
                .table(properties.getTable())
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(true)
                .load();
        flyway.migrate();
        log.info("Outbox Flyway migrations completed");
        return flyway;
    }
}