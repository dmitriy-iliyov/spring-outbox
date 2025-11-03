package io.github.dmitriyiliyov.springoutbox.core.aop;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface OutboxEvent {
    String eventType();
    @Language("SpEL")
    String payload() default "#result";
}