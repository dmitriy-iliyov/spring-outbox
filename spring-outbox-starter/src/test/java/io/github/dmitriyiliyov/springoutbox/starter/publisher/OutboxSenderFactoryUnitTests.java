package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxSender;
import io.github.dmitriyiliyov.springoutbox.kafka.KafkaOutboxSender;
import io.github.dmitriyiliyov.springoutbox.rabbit.RabbitOutboxSender;
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
    @DisplayName("UT create() when type is null should throw IAE")
    void create_whenTypeNull_shouldThrowIAE() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(null);

        // when + then
        assertThrows(IllegalArgumentException.class, () -> OutboxSenderFactory.create(props, context, mapper));
    }

    @Test
    @DisplayName("UT create() when type is KAFKA and beanName specified should return KafkaOutboxSender")
    void create_whenKafkaAndBeanNameSpecified_shouldReturnSender() {
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
        OutboxSender result = OutboxSenderFactory.create(props, context, mapper);

        // then
        assertInstanceOf(KafkaOutboxSender.class, result);
    }

    @Test
    @DisplayName("UT create() when type is KAFKA and beanName not specified should resolve by type")
    void create_whenKafkaAndBeanNameNotSpecified_shouldResolveByType() {
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
        OutboxSender result = OutboxSenderFactory.create(props, context, mapper);

        // then
        assertInstanceOf(KafkaOutboxSender.class, result);
    }

    @Test
    @DisplayName("UT create() when type is KAFKA and multiple beans found should throw ISE")
    void create_whenKafkaAndMultipleBeans_shouldThrowISE() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.KAFKA);

        when(context.getBeanNamesForType(KafkaTemplate.class)).thenReturn(new String[]{"kafka1", "kafka2"});

        // when + then
        assertThrows(IllegalStateException.class, () -> OutboxSenderFactory.create(props, context, mapper));
    }

    @Test
    @DisplayName("UT create() when type is KAFKA and no beans found should throw ISE")
    void create_whenKafkaAndNoBeans_shouldThrowISE() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.KAFKA);

        when(context.getBeanNamesForType(KafkaTemplate.class)).thenReturn(new String[]{});

        // when + then
        assertThrows(IllegalStateException.class, () -> OutboxSenderFactory.create(props, context, mapper));
    }

    @Test
    @DisplayName("UT create() when type is KAFKA and bean not found by name should throw IAE")
    void create_whenKafkaAndBeanNotFoundByName_shouldThrowIAE() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.KAFKA);
        props.setBeanName("missingBean");

        when(context.containsBean("missingBean")).thenReturn(false);

        // when + then
        assertThrows(IllegalArgumentException.class, () -> OutboxSenderFactory.create(props, context, mapper));
    }

    @Test
    @DisplayName("UT create() when type is RABBIT_MQ and beanName specified should return RabbitMqOutboxSender")
    void create_whenRabbitAndBeanNameSpecified_shouldReturnSender() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.RABBIT);
        props.setBeanName("rabbitTemplate");
        props.setEmergencyTimeout(Duration.ofSeconds(10));

        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        when(rabbitTemplate.isMandatoryFor(any(Message.class))).thenReturn(true);
        
        when(context.containsBean("rabbitTemplate")).thenReturn(true);
        when(context.getBean("rabbitTemplate", RabbitTemplate.class)).thenReturn(rabbitTemplate);

        // when
        OutboxSender result = OutboxSenderFactory.create(props, context, mapper);

        // then
        assertInstanceOf(RabbitOutboxSender.class, result);
    }

    @Test
    @DisplayName("UT create() when type is RABBIT_MQ and beanName not specified should resolve by type")
    void create_whenRabbitAndBeanNameNotSpecified_shouldResolveByType() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.RABBIT);
        props.setEmergencyTimeout(Duration.ofSeconds(10));

        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        when(rabbitTemplate.isMandatoryFor(any(Message.class))).thenReturn(false);

        when(context.getBeanNamesForType(RabbitTemplate.class)).thenReturn(new String[]{"rabbitTemplate"});
        when(context.containsBean("rabbitTemplate")).thenReturn(true);
        when(context.getBean("rabbitTemplate", RabbitTemplate.class)).thenReturn(rabbitTemplate);

        // when
        OutboxSender result = OutboxSenderFactory.create(props, context, mapper);

        // then
        assertInstanceOf(RabbitOutboxSender.class, result);
    }

    @Test
    @DisplayName("UT create() when type is RABBIT_MQ and multiple beans found should throw ISE")
    void create_whenRabbitAndMultipleBeans_shouldThrowISE() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.RABBIT);

        when(context.getBeanNamesForType(RabbitTemplate.class)).thenReturn(new String[]{"rabbit1", "rabbit2"});

        // when + then
        assertThrows(IllegalStateException.class, () -> OutboxSenderFactory.create(props, context, mapper));
    }

    @Test
    @DisplayName("UT create() when type is RABBIT_MQ and no beans found should throw ISE")
    void create_whenRabbitAndNoBeans_shouldThrowISE() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.RABBIT);

        when(context.getBeanNamesForType(RabbitTemplate.class)).thenReturn(new String[]{});

        // when + then
        assertThrows(IllegalStateException.class, () -> OutboxSenderFactory.create(props, context, mapper));
    }

    @Test
    @DisplayName("UT create() when type is RABBIT_MQ and bean not found by name should throw IAE")
    void create_whenRabbitAndBeanNotFoundByName_shouldThrowIAE() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.RABBIT);
        props.setBeanName("missingBean");

        when(context.containsBean("missingBean")).thenReturn(false);

        // when + then
        assertThrows(IllegalArgumentException.class, () -> OutboxSenderFactory.create(props, context, mapper));
    }

    @Test
    @DisplayName("UT create() when type is RABBIT_MQ and mandatory is false should still return RabbitMqOutboxSender")
    void create_whenRabbitAndMandatoryFalse_shouldReturnSender() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.RABBIT);
        props.setBeanName("rabbitTemplate");
        props.setEmergencyTimeout(Duration.ofSeconds(10));

        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        when(rabbitTemplate.isMandatoryFor(any(Message.class))).thenReturn(false);

        when(context.containsBean("rabbitTemplate")).thenReturn(true);
        when(context.getBean("rabbitTemplate", RabbitTemplate.class)).thenReturn(rabbitTemplate);

        // when
        OutboxSender result = OutboxSenderFactory.create(props, context, mapper);

        // then
        assertInstanceOf(RabbitOutboxSender.class, result);
    }

    @Test
    @DisplayName("UT create() when type is KAFKA and acks is not 'all' should still return KafkaOutboxSender")
    void create_whenKafkaAndAcksNotAll_shouldReturnSender() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.KAFKA);
        props.setBeanName("kafkaTemplate");
        props.setEmergencyTimeout(Duration.ofSeconds(10));

        KafkaTemplate kafkaTemplate = mock(KafkaTemplate.class);
        ProducerFactory producerFactory = mock(ProducerFactory.class);
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);
        when(producerFactory.getConfigurationProperties()).thenReturn(Map.of("acks", "1", "enable.idempotence", true));

        when(context.containsBean("kafkaTemplate")).thenReturn(true);
        when(context.getBean("kafkaTemplate", KafkaTemplate.class)).thenReturn(kafkaTemplate);

        // when
        OutboxSender result = OutboxSenderFactory.create(props, context, mapper);

        // then
        assertInstanceOf(KafkaOutboxSender.class, result);
    }

    @Test
    @DisplayName("UT create() when type is KAFKA and idempotence is false should still return KafkaOutboxSender")
    void create_whenKafkaAndIdempotenceFalse_shouldReturnSender() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.KAFKA);
        props.setBeanName("kafkaTemplate");
        props.setEmergencyTimeout(Duration.ofSeconds(10));

        KafkaTemplate kafkaTemplate = mock(KafkaTemplate.class);
        ProducerFactory producerFactory = mock(ProducerFactory.class);
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);
        when(producerFactory.getConfigurationProperties()).thenReturn(Map.of("acks", "all", "enable.idempotence", false));

        when(context.containsBean("kafkaTemplate")).thenReturn(true);
        when(context.getBean("kafkaTemplate", KafkaTemplate.class)).thenReturn(kafkaTemplate);

        // when
        OutboxSender result = OutboxSenderFactory.create(props, context, mapper);

        // then
        assertInstanceOf(KafkaOutboxSender.class, result);
    }

    @Test
    @DisplayName("UT create() when type is KAFKA and idempotence is String 'true' should return KafkaOutboxSender")
    void create_whenKafkaAndIdempotenceAsStringTrue_shouldReturnSender() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.KAFKA);
        props.setBeanName("kafkaTemplate");
        props.setEmergencyTimeout(Duration.ofSeconds(10));

        KafkaTemplate kafkaTemplate = mock(KafkaTemplate.class);
        ProducerFactory producerFactory = mock(ProducerFactory.class);
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);
        when(producerFactory.getConfigurationProperties()).thenReturn(Map.of("acks", "all", "enable.idempotence", "true"));

        when(context.containsBean("kafkaTemplate")).thenReturn(true);
        when(context.getBean("kafkaTemplate", KafkaTemplate.class)).thenReturn(kafkaTemplate);

        // when
        OutboxSender result = OutboxSenderFactory.create(props, context, mapper);

        // then
        assertInstanceOf(KafkaOutboxSender.class, result);
    }

    @Test
    @DisplayName("UT create() when type is KAFKA and idempotence is String 'false' should still return KafkaOutboxSender")
    void create_whenKafkaAndIdempotenceAsStringFalse_shouldReturnSender() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.KAFKA);
        props.setBeanName("kafkaTemplate");
        props.setEmergencyTimeout(Duration.ofSeconds(10));

        KafkaTemplate kafkaTemplate = mock(KafkaTemplate.class);
        ProducerFactory producerFactory = mock(ProducerFactory.class);
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);
        when(producerFactory.getConfigurationProperties()).thenReturn(Map.of("acks", "all", "enable.idempotence", "false"));

        when(context.containsBean("kafkaTemplate")).thenReturn(true);
        when(context.getBean("kafkaTemplate", KafkaTemplate.class)).thenReturn(kafkaTemplate);

        // when
        OutboxSender result = OutboxSenderFactory.create(props, context, mapper);

        // then
        assertInstanceOf(KafkaOutboxSender.class, result);
    }

    @Test
    @DisplayName("UT create() when type is KAFKA and beanName is empty should resolve by type")
    void create_whenKafkaAndBeanNameEmpty_shouldResolveByType() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.KAFKA);
        props.setBeanName("");
        props.setEmergencyTimeout(Duration.ofSeconds(10));

        KafkaTemplate kafkaTemplate = mock(KafkaTemplate.class);
        ProducerFactory producerFactory = mock(ProducerFactory.class);
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);
        when(producerFactory.getConfigurationProperties()).thenReturn(Map.of("acks", "all", "enable.idempotence", true));

        when(context.getBeanNamesForType(KafkaTemplate.class)).thenReturn(new String[]{"kafkaTemplate"});
        when(context.containsBean("kafkaTemplate")).thenReturn(true);
        when(context.getBean("kafkaTemplate", KafkaTemplate.class)).thenReturn(kafkaTemplate);

        // when
        OutboxSender result = OutboxSenderFactory.create(props, context, mapper);

        // then
        assertInstanceOf(KafkaOutboxSender.class, result);
    }

    @Test
    @DisplayName("UT create() when type is RABBIT_MQ and beanName is empty should resolve by type")
    void create_whenRabbitAndBeanNameEmpty_shouldResolveByType() {
        // given
        OutboxPublisherProperties.SenderProperties props = new OutboxPublisherProperties.SenderProperties();
        props.setType(SenderType.RABBIT);
        props.setBeanName("");
        props.setEmergencyTimeout(Duration.ofSeconds(10));

        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        when(rabbitTemplate.isMandatoryFor(any(Message.class))).thenReturn(true);

        when(context.getBeanNamesForType(RabbitTemplate.class)).thenReturn(new String[]{"rabbitTemplate"});
        when(context.containsBean("rabbitTemplate")).thenReturn(true);
        when(context.getBean("rabbitTemplate", RabbitTemplate.class)).thenReturn(rabbitTemplate);

        // when
        OutboxSender result = OutboxSenderFactory.create(props, context, mapper);

        // then
        assertInstanceOf(RabbitOutboxSender.class, result);
    }
}