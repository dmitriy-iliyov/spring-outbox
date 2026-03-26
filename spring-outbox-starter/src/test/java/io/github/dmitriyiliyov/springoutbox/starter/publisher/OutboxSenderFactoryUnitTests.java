package io.github.dmitriyiliyov.springoutbox.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxSender;
import io.github.dmitriyiliyov.springoutbox.kafka.KafkaOutboxSender;
import io.github.dmitriyiliyov.springoutbox.rabbit.RabbitMqOutboxSender;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherProperties;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxSenderFactory;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.SenderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxSenderFactoryUnitTests {

    @Mock
    ApplicationContext context;

    @Mock
    ObjectMapper mapper;

    @Test
    @DisplayName("UT generate() when type is null should throw IAE")
    void generate_whenTypeNull_shouldThrowIAE() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(null);

        // when + then
        assertThrows(IllegalArgumentException.class, () -> OutboxSenderFactory.generate(props, context, mapper));
    }

    @Test
    @DisplayName("UT generate() when type is KAFKA and beanName specified should return KafkaOutboxSender")
    void generate_whenKafkaAndBeanNameSpecified_shouldReturnSender() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.KAFKA);
        props.setBeanName("kafkaTemplate");
        props.setEmergencyTimeout(Duration.ofSeconds(10));

        KafkaTemplate kafkaTemplate = mock(KafkaTemplate.class);
        ProducerFactory producerFactory = mock(ProducerFactory.class);
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);
        when(producerFactory.getConfigurationProperties()).thenReturn(Map.of("acks", "all", "enable.idempotence", true));
        
        when(context.containsBean("kafkaTemplate")).thenReturn(true);
        when(context.getBean("kafkaTemplate", KafkaTemplate.class)).thenReturn(kafkaTemplate);

        // when
        OutboxSender result = OutboxSenderFactory.generate(props, context, mapper);

        // then
        assertInstanceOf(KafkaOutboxSender.class, result);
    }

    @Test
    @DisplayName("UT generate() when type is KAFKA and beanName not specified should resolve by type")
    void generate_whenKafkaAndBeanNameNotSpecified_shouldResolveByType() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.KAFKA);
        props.setEmergencyTimeout(Duration.ofSeconds(10));

        KafkaTemplate kafkaTemplate = mock(KafkaTemplate.class);
        ProducerFactory producerFactory = mock(ProducerFactory.class);
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);
        when(producerFactory.getConfigurationProperties()).thenReturn(Map.of());

        when(context.getBeanNamesForType(KafkaTemplate.class)).thenReturn(new String[]{"kafkaTemplate"});
        when(context.containsBean("kafkaTemplate")).thenReturn(true);
        when(context.getBean("kafkaTemplate", KafkaTemplate.class)).thenReturn(kafkaTemplate);

        // when
        OutboxSender result = OutboxSenderFactory.generate(props, context, mapper);

        // then
        assertInstanceOf(KafkaOutboxSender.class, result);
    }

    @Test
    @DisplayName("UT generate() when type is KAFKA and multiple beans found should throw ISE")
    void generate_whenKafkaAndMultipleBeans_shouldThrowISE() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.KAFKA);

        when(context.getBeanNamesForType(KafkaTemplate.class)).thenReturn(new String[]{"kafka1", "kafka2"});

        // when + then
        assertThrows(IllegalStateException.class, () -> OutboxSenderFactory.generate(props, context, mapper));
    }

    @Test
    @DisplayName("UT generate() when type is KAFKA and no beans found should throw ISE")
    void generate_whenKafkaAndNoBeans_shouldThrowISE() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.KAFKA);

        when(context.getBeanNamesForType(KafkaTemplate.class)).thenReturn(new String[]{});

        // when + then
        assertThrows(IllegalStateException.class, () -> OutboxSenderFactory.generate(props, context, mapper));
    }

    @Test
    @DisplayName("UT generate() when type is KAFKA and bean not found by name should throw IAE")
    void generate_whenKafkaAndBeanNotFoundByName_shouldThrowIAE() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.KAFKA);
        props.setBeanName("missingBean");

        when(context.containsBean("missingBean")).thenReturn(false);

        // when + then
        assertThrows(IllegalArgumentException.class, () -> OutboxSenderFactory.generate(props, context, mapper));
    }

    @Test
    @DisplayName("UT generate() when type is RABBIT_MQ and beanName specified should return RabbitMqOutboxSender")
    void generate_whenRabbitAndBeanNameSpecified_shouldReturnSender() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.RABBIT_MQ);
        props.setBeanName("rabbitTemplate");
        props.setEmergencyTimeout(Duration.ofSeconds(10));

        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        when(rabbitTemplate.isMandatoryFor(any(Message.class))).thenReturn(true);
        
        when(context.containsBean("rabbitTemplate")).thenReturn(true);
        when(context.getBean("rabbitTemplate", RabbitTemplate.class)).thenReturn(rabbitTemplate);

        // when
        OutboxSender result = OutboxSenderFactory.generate(props, context, mapper);

        // then
        assertInstanceOf(RabbitMqOutboxSender.class, result);
    }

    @Test
    @DisplayName("UT generate() when type is RABBIT_MQ and beanName not specified should resolve by type")
    void generate_whenRabbitAndBeanNameNotSpecified_shouldResolveByType() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.RABBIT_MQ);
        props.setEmergencyTimeout(Duration.ofSeconds(10));

        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        when(rabbitTemplate.isMandatoryFor(any(Message.class))).thenReturn(false);

        when(context.getBeanNamesForType(RabbitTemplate.class)).thenReturn(new String[]{"rabbitTemplate"});
        when(context.containsBean("rabbitTemplate")).thenReturn(true);
        when(context.getBean("rabbitTemplate", RabbitTemplate.class)).thenReturn(rabbitTemplate);

        // when
        OutboxSender result = OutboxSenderFactory.generate(props, context, mapper);

        // then
        assertInstanceOf(RabbitMqOutboxSender.class, result);
    }

    @Test
    @DisplayName("UT generate() when type is RABBIT_MQ and multiple beans found should throw ISE")
    void generate_whenRabbitAndMultipleBeans_shouldThrowISE() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.RABBIT_MQ);

        when(context.getBeanNamesForType(RabbitTemplate.class)).thenReturn(new String[]{"rabbit1", "rabbit2"});

        // when + then
        assertThrows(IllegalStateException.class, () -> OutboxSenderFactory.generate(props, context, mapper));
    }

    @Test
    @DisplayName("UT generate() when type is RABBIT_MQ and no beans found should throw ISE")
    void generate_whenRabbitAndNoBeans_shouldThrowISE() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.RABBIT_MQ);

        when(context.getBeanNamesForType(RabbitTemplate.class)).thenReturn(new String[]{});

        // when + then
        assertThrows(IllegalStateException.class, () -> OutboxSenderFactory.generate(props, context, mapper));
    }
}
