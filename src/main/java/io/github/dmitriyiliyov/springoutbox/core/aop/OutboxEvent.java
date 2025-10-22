package io.github.dmitriyiliyov.springoutbox.core.aop;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface OutboxEvent {
    String eventType();
    String payload() default "#result";
}