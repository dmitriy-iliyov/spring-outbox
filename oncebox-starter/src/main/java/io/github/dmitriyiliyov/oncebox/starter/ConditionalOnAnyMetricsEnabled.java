package io.github.dmitriyiliyov.oncebox.starter;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnAnyMetricsEnabledCondition.class)
@Documented
public @interface ConditionalOnAnyMetricsEnabled { }
