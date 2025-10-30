package io.github.dmitriyiliyov.springoutbox.unit.config;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import io.github.dmitriyiliyov.springoutbox.config.SenderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class OutboxPropertiesSenderPropertiesUnitTests {

    @Test
    @DisplayName("UT SenderProperties() should throw when type is null")
    public void constructor_whenTypeNull_shouldThrow() {
        // given
        SenderType type = null;
        String beanName = "myBean";

        // when + then
        assertThrows(NullPointerException.class, () -> new OutboxProperties.SenderProperties(type, beanName));
    }

    @Test
    @DisplayName("UT SenderProperties() should throw when beanName is null")
    public void constructor_whenBeanNameNull_shouldThrow() {
        // given
        SenderType type = SenderType.KAFKA;
        String beanName = null;

        // when + then
        assertThrows(NullPointerException.class, () -> new OutboxProperties.SenderProperties(type, beanName));
    }

    @Test
    @DisplayName("UT SenderProperties() should throw when beanName is blank")
    public void constructor_whenBeanNameBlank_shouldThrow() {
        // given
        SenderType type = SenderType.KAFKA;
        String beanName = "  ";

        // when + then
        assertThrows(IllegalArgumentException.class, () -> new OutboxProperties.SenderProperties(type, beanName));
    }

    @Test
    @DisplayName("UT SenderProperties() with valid type and beanName should create object")
    public void constructor_withValidParameters_shouldCreateObject() {
        // given
        SenderType type = SenderType.KAFKA;
        String beanName = "myKafkaBean";

        // when
        OutboxProperties.SenderProperties sender = new OutboxProperties.SenderProperties(type, beanName);

        // then
        assert type == sender.type();
        assert beanName.equals(sender.beanName());
    }
}
