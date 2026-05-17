package io.github.dmitriyiliyov.springoutbox.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.List;

public class CompositePostApplicationReadyOutboxInitializer implements PostApplicationReadyOutboxInitializer {

    private static final Logger log = LoggerFactory.getLogger(CompositePostApplicationReadyOutboxInitializer.class);

    private final OutboxProperties properties;
    private final List<PostApplicationReadyOutboxInitializer> initializers;

    public CompositePostApplicationReadyOutboxInitializer(OutboxProperties properties,
                                                          List<PostApplicationReadyOutboxInitializer> initializers) {
        this.properties = properties;
        this.initializers = initializers;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Override
    public void init() {
        initializers.forEach(PostApplicationReadyOutboxInitializer::init);

        boolean isPublisherEnabled = properties.getPublisher().isEnabled();
        boolean isConsumerEnabled = properties.getConsumer().isEnabled();

        if (isPublisherEnabled && !isConsumerEnabled) {
            log.debug(LogUtils.prettyPrint(properties.toStringWithPublisher()));
        } else if (!isPublisherEnabled && isConsumerEnabled) {
            log.debug(LogUtils.prettyPrint(properties.toStringWithConsumer()));
        } else {
            log.debug(LogUtils.prettyPrint(properties));
        }
    }
}
