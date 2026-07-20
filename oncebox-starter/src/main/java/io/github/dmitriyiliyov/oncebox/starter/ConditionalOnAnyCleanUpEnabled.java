package io.github.dmitriyiliyov.oncebox.starter;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnAnyCleanUpEnabledCondition.class)
@Documented
public @interface ConditionalOnAnyCleanUpEnabled { }
