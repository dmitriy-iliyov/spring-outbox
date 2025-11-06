package io.github.dmitriyiliyov.springoutbox.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({
        OutboxAutoConfiguration.class,
        OutboxDlqAutoConfiguration.class
})
public @interface EnableOutbox { }