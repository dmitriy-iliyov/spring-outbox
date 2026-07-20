package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.starter.consumer.OutboxConsumerAutoConfiguration;
import io.github.dmitriyiliyov.oncebox.starter.publisher.OutboxPublisherAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({
        OutboxAutoConfiguration.class,
        OutboxPublisherAutoConfiguration.class,
        OutboxConsumerAutoConfiguration.class
})
public @interface EnableOutbox { }