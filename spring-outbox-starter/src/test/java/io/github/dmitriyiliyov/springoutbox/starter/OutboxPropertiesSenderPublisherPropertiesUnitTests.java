package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherProperties;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.SenderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OutboxPropertiesSenderPublisherPropertiesUnitTests {

    @Test
    @DisplayName("UT initialize() should throw when type is null")
    public void init_typeNull_shouldThrow() {
        // given
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(null);
        sender.setBeanName("myBean");

        // when + then
        assertThrows(IllegalArgumentException.class, sender::init);
    }

    @Test
    @DisplayName("UT initialize() with valid type and beanName should assign values")
    public void init_withValidTypeAndBeanName_shouldAssignValues() {
        // given
        SenderType type = SenderType.KAFKA;
        String beanName = "myKafkaBean";

        // when
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(type);
        sender.setBeanName(beanName);
        sender.init();

        // then
        assertEquals(type, sender.getType());
        assertEquals(beanName, sender.getBeanName());
        assertEquals(Duration.ofSeconds(120), sender.getEmergencyTimeout());
    }

    @Test
    @DisplayName("UT initialize() with valid type, beanName and emergencyTimeout should assign values")
    public void init_validParameters_shouldAssignValues() {
        // given
        SenderType type = SenderType.KAFKA;
        String beanName = "myKafkaBean";
        Duration emergencyTimeout = Duration.ofSeconds(120);

        // when
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(type);
        sender.setBeanName(beanName);
        sender.setEmergencyTimeout(emergencyTimeout);
        sender.init();

        // then
        assertEquals(type, sender.getType());
        assertEquals(beanName, sender.getBeanName());
        assertEquals(emergencyTimeout, sender.getEmergencyTimeout());
    }
}
