package io.github.dmitriyiliyov.springoutbox.starter;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnAnyMetricsEnabledCondition.class)
public @interface ConditionalOnAnyMetricsEnabled { }
