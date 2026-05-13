package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxSender;
import io.github.dmitriyiliyov.springoutbox.kafka.KafkaOutboxSender;
import io.github.dmitriyiliyov.springoutbox.rabbit.RabbitOutboxSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxSenderAutoConfigurationUnitTests {

    private OutboxPublisherAutoConfiguration config;

    @Mock
    private OutboxPublisherProperties publisherProperties;

    @Mock
    private OutboxPublisherProperties.SenderProperties senderProperties;

    @Mock
    private ApplicationContext context;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ProducerFactory<String, Object> producerFactory;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        config = new OutboxPublisherAutoConfiguration(publisherProperties, null);
        lenient().when(publisherProperties.getSender()).thenReturn(senderProperties);
        lenient().when(senderProperties.getEmergencyTimeout()).thenReturn(Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("UT kafkaOutboxSender creates sender with explicit bean name and boolean idempotence")
    void kafkaOutboxSender_withExplicitBeanName_createsSuccessfully() {
        when(senderProperties.getBeanName()).thenReturn("explicitKafkaBean");
        when(context.containsBean("explicitKafkaBean")).thenReturn(true);
        when(context.getBean("explicitKafkaBean", KafkaTemplate.class)).thenReturn(kafkaTemplate);
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);
        when(producerFactory.getConfigurationProperties()).thenReturn(Map.of(
                "acks", "all",
                "enable.idempotence", true
        ));

        OutboxSender result = config.kafkaOutboxSender(context);

        assertThat(result).isInstanceOf(KafkaOutboxSender.class);
        verify(senderProperties).setBeanName("explicitKafkaBean");
    }

    @Test
    @DisplayName("UT kafkaOutboxSender creates sender with implicit type resolution and string idempotence")
    void kafkaOutboxSender_withImplicitTypeResolution_createsSuccessfully() {
        when(senderProperties.getBeanName()).thenReturn(null);
        when(context.getBeanNamesForType(KafkaTemplate.class)).thenReturn(new String[]{"resolvedKafkaBean"});
        when(context.containsBean("resolvedKafkaBean")).thenReturn(true);
        when(context.getBean("resolvedKafkaBean", KafkaTemplate.class)).thenReturn(kafkaTemplate);
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);
        when(producerFactory.getConfigurationProperties()).thenReturn(Map.of(
                "acks", "all",
                "enable.idempotence", "true"
        ));

        OutboxSender result = config.kafkaOutboxSender(context);

        assertThat(result).isInstanceOf(KafkaOutboxSender.class);
        verify(senderProperties).setBeanName("resolvedKafkaBean");
    }

    @Test
    @DisplayName("UT kafkaOutboxSender creates sender with warnings when acks and idempotence are misconfigured")
    void kafkaOutboxSender_withMisconfiguredProperties_createsSuccessfullyWithWarnings() {
        when(senderProperties.getBeanName()).thenReturn("warningKafkaBean");
        when(context.containsBean("warningKafkaBean")).thenReturn(true);
        when(context.getBean("warningKafkaBean", KafkaTemplate.class)).thenReturn(kafkaTemplate);
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);
        when(producerFactory.getConfigurationProperties()).thenReturn(Map.of(
                "acks", "1",
                "enable.idempotence", false
        ));

        OutboxSender result = config.kafkaOutboxSender(context);

        assertThat(result).isInstanceOf(KafkaOutboxSender.class);
    }

    @Test
    @DisplayName("UT kafkaOutboxSender creates sender with warnings when configs are missing completely")
    void kafkaOutboxSender_withMissingProperties_createsSuccessfullyWithWarnings() {
        when(senderProperties.getBeanName()).thenReturn("emptyKafkaBean");
        when(context.containsBean("emptyKafkaBean")).thenReturn(true);
        when(context.getBean("emptyKafkaBean", KafkaTemplate.class)).thenReturn(kafkaTemplate);
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);
        when(producerFactory.getConfigurationProperties()).thenReturn(Map.of());

        OutboxSender result = config.kafkaOutboxSender(context);

        assertThat(result).isInstanceOf(KafkaOutboxSender.class);
    }

    @Test
    @DisplayName("UT kafkaOutboxSender throws IllegalStateException when implicit resolution finds zero beans")
    void kafkaOutboxSender_implicitResolutionZeroBeans_throwsException() {
        when(senderProperties.getBeanName()).thenReturn("");
        when(context.getBeanNamesForType(KafkaTemplate.class)).thenReturn(new String[0]);

        assertThatThrownBy(() -> config.kafkaOutboxSender(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot create OutboxSender: no KafkaTemplate bean found");
    }

    @Test
    @DisplayName("UT kafkaOutboxSender throws IllegalStateException when implicit resolution finds multiple beans")
    void kafkaOutboxSender_implicitResolutionMultipleBeans_throwsException() {
        when(senderProperties.getBeanName()).thenReturn(null);
        when(context.getBeanNamesForType(KafkaTemplate.class)).thenReturn(new String[]{"bean1", "bean2"});

        assertThatThrownBy(() -> config.kafkaOutboxSender(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot create OutboxSender: found more then one KafkaTemplate bean");
    }

    @Test
    @DisplayName("UT kafkaOutboxSender throws IllegalArgumentException when context does not contain resolved bean")
    void kafkaOutboxSender_beanNotFoundInContext_throwsException() {
        when(senderProperties.getBeanName()).thenReturn("missingKafkaBean");
        when(context.containsBean("missingKafkaBean")).thenReturn(false);

        assertThatThrownBy(() -> config.kafkaOutboxSender(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot create OutboxSender: KafkaTemplate bean 'missingKafkaBean' not found");
    }

    @Test
    @DisplayName("UT rabbitOutboxSender creates sender with explicit bean name and mandatory flag true")
    void rabbitOutboxSender_withExplicitBeanName_createsSuccessfully() {
        when(senderProperties.getBeanName()).thenReturn("explicitRabbitBean");
        when(context.containsBean("explicitRabbitBean")).thenReturn(true);
        when(context.getBean("explicitRabbitBean", RabbitTemplate.class)).thenReturn(rabbitTemplate);
        when(rabbitTemplate.isMandatoryFor(any())).thenReturn(true);

        OutboxSender result = config.rabbitOutboxSender(context);

        assertThat(result).isInstanceOf(RabbitOutboxSender.class);
        verify(senderProperties).setBeanName("explicitRabbitBean");
    }

    @Test
    @DisplayName("UT rabbitOutboxSender creates sender with implicit type resolution and mandatory flag false")
    void rabbitOutboxSender_withImplicitTypeResolution_createsSuccessfullyWithWarning() {
        when(senderProperties.getBeanName()).thenReturn("");
        when(context.getBeanNamesForType(RabbitTemplate.class)).thenReturn(new String[]{"resolvedRabbitBean"});
        when(context.containsBean("resolvedRabbitBean")).thenReturn(true);
        when(context.getBean("resolvedRabbitBean", RabbitTemplate.class)).thenReturn(rabbitTemplate);
        when(rabbitTemplate.isMandatoryFor(any())).thenReturn(false);

        OutboxSender result = config.rabbitOutboxSender(context);

        assertThat(result).isInstanceOf(RabbitOutboxSender.class);
        verify(senderProperties).setBeanName("resolvedRabbitBean");
    }

    @Test
    @DisplayName("UT rabbitOutboxSender throws IllegalStateException when implicit resolution finds zero beans")
    void rabbitOutboxSender_implicitResolutionZeroBeans_throwsException() {
        when(senderProperties.getBeanName()).thenReturn(null);
        when(context.getBeanNamesForType(RabbitTemplate.class)).thenReturn(new String[0]);

        assertThatThrownBy(() -> config.rabbitOutboxSender(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot create OutboxSender: no RabbitTemplate bean found");
    }

    @Test
    @DisplayName("UT rabbitOutboxSender throws IllegalStateException when implicit resolution finds multiple beans")
    void rabbitOutboxSender_implicitResolutionMultipleBeans_throwsException() {
        when(senderProperties.getBeanName()).thenReturn("");
        when(context.getBeanNamesForType(RabbitTemplate.class)).thenReturn(new String[]{"rabbit1", "rabbit2"});

        assertThatThrownBy(() -> config.rabbitOutboxSender(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot create OutboxSender: found more then one RabbitTemplate bean");
    }

    @Test
    @DisplayName("UT rabbitOutboxSender throws IllegalArgumentException when context does not contain resolved bean")
    void rabbitOutboxSender_beanNotFoundInContext_throwsException() {
        when(senderProperties.getBeanName()).thenReturn("missingRabbitBean");
        when(context.containsBean("missingRabbitBean")).thenReturn(false);

        assertThatThrownBy(() -> config.rabbitOutboxSender(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot create OutboxSender: RabbitTemplate bean 'missingRabbitBean' not found");
    }
}
