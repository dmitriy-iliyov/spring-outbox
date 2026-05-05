package io.github.dmitriyiliyov.springoutbox.starter;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnDatabaseTypeCondition.class)
@Documented
public @interface ConditionalOnDatabaseType {
    DatabaseType type();
}
