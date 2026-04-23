package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.starter.consumer.OutboxConsumerProperties;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherProperties;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.SenderType;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class LogUtilsUnitTests {

    @Test
    public void prettyPrint_test() {
        OutboxProperties properties = new OutboxProperties();

        OutboxPublisherProperties publisherProperties = new OutboxPublisherProperties();
        publisherProperties.setEnabled(true);

        OutboxPublisherProperties.SenderProperties senderProperties = new OutboxPublisherProperties.SenderProperties();
        senderProperties.setType(SenderType.KAFKA);

        publisherProperties.setSender(senderProperties);
        publisherProperties.setEvents(Map.of());

        OutboxConsumerProperties consumerProperties = new OutboxConsumerProperties();
        consumerProperties.setEnabled(true);

        properties.setPublisher(publisherProperties);
        properties.setConsumer(consumerProperties);

        properties.applyDefaults();

        System.out.println(LogUtils.prettyPrint(properties));
    }
}
