package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.starter.consumer.OutboxConsumerProperties;
import io.github.dmitriyiliyov.oncebox.starter.publisher.OutboxPublisherProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class LogUtilsUnitTests {

    @Test
    public void prettyPrint_test() {
        OutboxProperties properties = new OutboxProperties();

        OutboxPublisherProperties publisherProperties = new OutboxPublisherProperties();
        publisherProperties.setEnabled(true);

        OutboxPublisherProperties.SenderProperties senderProperties = new OutboxPublisherProperties.SenderProperties();
        senderProperties.setType(TransportType.KAFKA);

        publisherProperties.setSender(senderProperties);
        publisherProperties.setEvents(Map.of());

        OutboxConsumerProperties consumerProperties = new OutboxConsumerProperties();
        consumerProperties.setEnabled(true);

        OutboxConsumerProperties.SourceProperties source = new OutboxConsumerProperties.SourceProperties();
        source.setType(TransportType.KAFKA);

        consumerProperties.setSource(source);

        properties.setPublisher(publisherProperties);
        properties.setConsumer(consumerProperties);

        properties.applyDefaults();

        System.out.println(LogUtils.prettyPrint(properties));
    }
}
