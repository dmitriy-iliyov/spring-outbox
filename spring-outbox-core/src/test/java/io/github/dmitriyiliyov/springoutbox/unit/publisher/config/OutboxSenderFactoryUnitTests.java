package io.github.dmitriyiliyov.springoutbox.unit.publisher.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.publisher.KafkaOutboxSender;
import io.github.dmitriyiliyov.springoutbox.publisher.OutboxSender;
import io.github.dmitriyiliyov.springoutbox.publisher.RabbitMqOutboxSender;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxSenderFactory;
import io.github.dmitriyiliyov.springoutbox.publisher.config.SenderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OutboxSenderFactoryUnitTests {

    @Test
    @DisplayName("UT generate() with null sender type should throw IllegalArgumentException")
    public void generate_nullType_shouldThrow() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(null);

        ApplicationContext context = mock(ApplicationContext.class);
        ObjectMapper mapper = new ObjectMapper();

        // when + then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> OutboxSenderFactory.generate(props, context, mapper));

        assertTrue(ex.getMessage().contains("Sender type is required"));
    }

    @Test
    @DisplayName("UT generate() with RabbitMq sender, valid bean and emergencyTimeout should return KafkaOutboxSender")
    public void generate_rabbitMqValidBean_shouldReturnRabbitMq() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.RABBIT_MQ);
        props.setBeanName("rabbitMqTemplate");
        props.setEmergencyTimeout(Duration.ofSeconds(120));

        ApplicationContext context = mock(ApplicationContext.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        when(rabbitTemplate.isMandatoryFor(any())).thenReturn(true);

        when(context.containsBean("rabbitMqTemplate")).thenReturn(true);
        when(context.getBean("rabbitMqTemplate", RabbitTemplate.class)).thenReturn(rabbitTemplate);

        ObjectMapper mapper = new ObjectMapper();

        // when
        OutboxSender sender = OutboxSenderFactory.generate(props, context, mapper);

        // then
        assertNotNull(sender);
        assertTrue(sender instanceof RabbitMqOutboxSender);
    }

    @Test
    @DisplayName("UT generate() with RabbitMq sender and missing bean should throw IllegalStateException")
    public void generate_rabbitMqMissingBean_shouldThrow() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.RABBIT_MQ);
        props.setBeanName("missingBean");

        ApplicationContext context = mock(ApplicationContext.class);
        when(context.containsBean("missingBean")).thenReturn(false);

        ObjectMapper mapper = new ObjectMapper();

        // when + then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> OutboxSenderFactory.generate(props, context, mapper));

        assertTrue(ex.getMessage().contains("RabbitTemplate bean 'missingBean' not found"));
    }

    @Test
    @DisplayName("UT generate() with Kafka sender, valid bean and emergencyTimeout should return KafkaOutboxSender")
    public void generate_kafkaValidBean_shouldReturnKafkaSender() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.KAFKA);
        props.setBeanName("kafkaTemplate");
        props.setEmergencyTimeout(Duration.ofSeconds(120));

        ApplicationContext context = mock(ApplicationContext.class);
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        ProducerFactory<String, Object> producerFactory = mock(ProducerFactory.class);

        when(context.containsBean("kafkaTemplate")).thenReturn(true);
        when(context.getBean("kafkaTemplate", KafkaTemplate.class)).thenReturn(kafkaTemplate);
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);
        when(producerFactory.getConfigurationProperties()).thenReturn(Map.of(
                "acks", "all",
                "enabled.idempotence", true
        ));

        ObjectMapper mapper = new ObjectMapper();

        // when
        OutboxSender sender = OutboxSenderFactory.generate(props, context, mapper);

        // then
        assertNotNull(sender);
        assertTrue(sender instanceof KafkaOutboxSender);
    }

    @Test
    @DisplayName("UT generate() with Kafka sender and missing bean should throw IllegalStateException")
    public void generate_kafkaMissingBean_shouldThrow() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.KAFKA);
        props.setBeanName("missingBean");

        ApplicationContext context = mock(ApplicationContext.class);
        when(context.containsBean("missingBean")).thenReturn(false);

        ObjectMapper mapper = new ObjectMapper();

        // when + then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> OutboxSenderFactory.generate(props, context, mapper));

        assertTrue(ex.getMessage().contains("KafkaTemplate bean 'missingBean' not found"));
    }
}
