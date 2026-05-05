package io.github.dmitriyiliyov.springoutbox.starter;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnAnyCleanUpEnabledCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Boolean isPublisherCleanUpEnabled = context.getEnvironment().getProperty("outbox.publisher.clean-up.enabled", Boolean.class);
        Boolean isConsumerCleanUpEnabled = context.getEnvironment().getProperty("outbox.consumer.clean-up.enabled", Boolean.class);
        if (isPublisherCleanUpEnabled == null || isPublisherCleanUpEnabled) {
            return ConditionOutcome.match();
        }
        if (isConsumerCleanUpEnabled != null && isConsumerCleanUpEnabled) {
            return ConditionOutcome.match();
        }
        return ConditionOutcome.noMatch("Nobody has the clean-up function enabled");
    }
}
