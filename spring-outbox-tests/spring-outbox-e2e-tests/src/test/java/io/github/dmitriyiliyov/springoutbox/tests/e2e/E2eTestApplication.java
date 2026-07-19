package io.github.dmitriyiliyov.springoutbox.tests.e2e;

import io.github.dmitriyiliyov.springoutbox.starter.EnableOutbox;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.config.E2eTestConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Single application acting as both publisher and consumer.
 * Events still travel through a real broker, so the whole lifecycle is exercised:
 * business transaction -> outbox polling -> broker -> idempotent consumer.
 */
@SpringBootApplication
@EnableTransactionManagement
@EnableOutbox
@Import(E2eTestConfig.class)
public class E2eTestApplication { }
