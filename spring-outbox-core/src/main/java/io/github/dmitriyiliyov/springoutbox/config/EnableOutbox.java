package io.github.dmitriyiliyov.springoutbox.config;

import io.github.dmitriyiliyov.springoutbox.consumer.config.OutboxConsumerAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxDlqAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({
        OutboxAutoConfiguration.class,
        OutboxPublisherAutoConfiguration.class,
        OutboxDlqAutoConfiguration.class,
        OutboxConsumerAutoConfiguration.class
})
public @interface EnableOutbox { }