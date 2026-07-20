package io.github.dmitriyiliyov.oncebox.starter;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnAnyMetricsEnabledCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();
        boolean isPublisherMetricsEnabled = environment.getProperty("oncebox.publisher.metrics.enabled", Boolean.class, false);
        boolean isConsumerMetricsEnabled = environment.getProperty("oncebox.consumer.metrics.enabled", Boolean.class, false);
        if (isPublisherMetricsEnabled || isConsumerMetricsEnabled) {
            return ConditionOutcome.match("At least somebody enabled metrics");
        }
        return ConditionOutcome.noMatch("Nobody has the metrics enabled");
    }
}
