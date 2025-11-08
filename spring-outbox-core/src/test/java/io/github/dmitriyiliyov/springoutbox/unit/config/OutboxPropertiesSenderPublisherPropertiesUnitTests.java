package io.github.dmitriyiliyov.springoutbox.unit.config;

import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties;
import io.github.dmitriyiliyov.springoutbox.publisher.config.SenderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OutboxPropertiesSenderPublisherPropertiesUnitTests {

    @Test
    @DisplayName("UT initialize() should throw when type is null")
    public void initialize_typeNull_shouldThrow() {
        // given
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(null);
        sender.setBeanName("myBean");

        // when + then
        assertThrows(IllegalArgumentException.class, sender::initialize);
    }

    @Test
    @DisplayName("UT initialize() with valid type and beanName should assign values")
    public void initialize_validParameters_shouldAssignValues() {
        // given
        SenderType type = SenderType.KAFKA;
        String beanName = "myKafkaBean";

        // when
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(type);
        sender.setBeanName(beanName);
        sender.initialize();

        // then
        assertEquals(type, sender.getType());
        assertEquals(beanName, sender.getBeanName());
    }
}
