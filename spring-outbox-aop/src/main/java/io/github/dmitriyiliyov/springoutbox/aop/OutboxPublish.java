package io.github.dmitriyiliyov.springoutbox.aop;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.*;

/**
 * Annotation to mark a method for automatic outbox event publishing.
 * <p>
 * When a method annotated with this is executed, the return value (or a derived value using SpEL)
 * is published as an outbox event.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface OutboxPublish {

    String eventType();

    @Language("SpEL")
    String payload() default "#result";
}
