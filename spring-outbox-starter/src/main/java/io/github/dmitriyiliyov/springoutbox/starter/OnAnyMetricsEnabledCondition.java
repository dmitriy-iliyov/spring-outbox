package io.github.dmitriyiliyov.springoutbox.starter;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnAnyMetricsEnabledCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();
        boolean isPublisherMetricsEnabled = environment.getProperty("outbox.publisher.metrics.enabled", Boolean.class, false);
        boolean isConsumerMetricsEnabled = environment.getProperty("outbox.consumer.metrics.enabled", Boolean.class, false);
        if (isPublisherMetricsEnabled || isConsumerMetricsEnabled) {
            return ConditionOutcome.match("At least one metrics module is enabled");
        }
        return ConditionOutcome.noMatch("No metrics modules enabled");
    }
}
