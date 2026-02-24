package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxSender;
import io.github.dmitriyiliyov.springoutbox.kafka.KafkaOutboxSender;
import io.github.dmitriyiliyov.springoutbox.rabbit.RabbitMqOutboxSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

/**
 * A factory for creating {@link OutboxSender} instances based on the configured message broker type.
 * <p>
 * It supports Apache Kafka and RabbitMQ and validates the configuration of their respective templates.
 */
public final class OutboxSenderFactory {

    private static final Logger log = LoggerFactory.getLogger(OutboxSenderFactory.class);
    private static final Map<SenderType, OutboxSenderSupplier> SUPPORTED_BROKERS = Map.of(
            SenderType.KAFKA, new KafkaOutboxSenderSupplier(),
            SenderType.RABBIT_MQ, new RabbitMqOutboxSenderSupplier()
    );

    private OutboxSenderFactory() {}

    /**
     * Generates an {@link OutboxSender} instance based on the provided properties.
     *
     * @param properties                The sender configuration properties.
     * @param context                   The Spring application context to resolve broker templates.
     * @param mapper                    The ObjectMapper for serialization.
     * @return                          A configured {@link OutboxSender} instance.
     * @throws IllegalArgumentException if the sender type is not specified or unsupported, or if the required bean is not found.
     * @throws IllegalStateException    if multiple beans of the required template type are found without a specific bean name.
     */
    public static OutboxSender generate(OutboxPublisherProperties.SenderProperties properties,
                                        ApplicationContext context,
                                        ObjectMapper mapper) {
        SenderType type = properties.getType();
        if (type == null) {
            throw new IllegalArgumentException(
                    "Sender type is required. Please set 'outbox.sender.type' property"
            );
        }
        OutboxSenderSupplier supplier = SUPPORTED_BROKERS.get(type);
        if (supplier == null) {
            throw new IllegalArgumentException(
                    "Unsupported sender type: " + type + ". Supported types: " + SUPPORTED_BROKERS.keySet()
            );
        }
        return supplier.supply(properties, context, mapper);
    }

    @FunctionalInterface
    private interface OutboxSenderSupplier {
        OutboxSender supply(OutboxPublisherProperties.SenderProperties properties,
                            ApplicationContext context,
                            ObjectMapper mapper);
    }

    private static class KafkaOutboxSenderSupplier implements OutboxSenderSupplier {

        @Override
        public OutboxSender supply(OutboxPublisherProperties.SenderProperties properties,
                                   ApplicationContext context,
                                   ObjectMapper mapper) {
            String beanName = properties.getBeanName();
            KafkaTemplate<String, Object> kafkaTemplate;
            if (beanName == null || beanName.isEmpty()) {
                log.warn("Sender bean-name is not specified. Will try to resolve by type");
                String [] beanNames = context.getBeanNamesForType(KafkaTemplate.class);
                if (beanNames.length == 0) {
                    throw new IllegalStateException("Cannot create OutboxSender: no KafkaTemplate bean found");
                }
                if (beanNames.length > 1) {
                    throw new IllegalStateException(
                            "Cannot create OutboxSender: found more then one KafkaTemplate bean: " +
                                    Arrays.toString(beanNames) +
                                    "Please define a KafkaTemplate<String, Object> bean with this name, " +
                                    "or configure 'outbox.sender.bean-name' property"
                    );
                }
                beanName = beanNames[0];
            }
            if (!context.containsBean(beanName)) {
                throw new IllegalArgumentException(
                        "Cannot create OutboxSender: KafkaTemplate bean '" + beanName + "' not found. " +
                                "Please define a KafkaTemplate<String, Object> bean with this name, " +
                                "or configure 'outbox.sender.bean-name' property"
                );
            }
            properties.setBeanName(beanName);
            kafkaTemplate = context.getBean(beanName, KafkaTemplate.class);
            Map<String, Object> configs = kafkaTemplate.getProducerFactory().getConfigurationProperties();
            String acks = (String) configs.get("acks");
            if (acks == null || !acks.equals("all")) {
                log.warn("Kafka producer factory is configured without 'acks=all'. Outbox cannot guarantee at-least-once delivery");
            }
            Boolean idempotence = null;
            Object idempotenceObj = configs.get("enable.idempotence");
            if (idempotenceObj instanceof Boolean) {
                idempotence = (Boolean) idempotenceObj;
            } else if (idempotenceObj instanceof String) {
                idempotence = Boolean.parseBoolean((String) idempotenceObj);
            }
            if (idempotence == null || !idempotence) {
                log.warn("Kafka producer is not idempotent. It is recommended to enabled 'enabled.idempotence=true' to avoid message duplication");
            }
            return new KafkaOutboxSender(kafkaTemplate, properties.getEmergencyTimeout().toSeconds(), mapper);
        }
    }

    private static class RabbitMqOutboxSenderSupplier implements OutboxSenderSupplier {

        @Override
        public OutboxSender supply(OutboxPublisherProperties.SenderProperties properties,
                                   ApplicationContext context,
                                   ObjectMapper mapper) {
            String beanName = properties.getBeanName();
            RabbitTemplate rabbitTemplate;
            if (beanName == null || beanName.isEmpty()) {
                log.warn("Sender bean-name is not specified. Will try to resolve by type");
                String [] beanNames = context.getBeanNamesForType(RabbitTemplate.class);
                if (beanNames.length == 0) {
                    throw new IllegalStateException("Cannot create OutboxSender: no RabbitTemplate bean found");
                }
                if (beanNames.length > 1) {
                    throw new IllegalStateException(
                            "Cannot create OutboxSender: found more then one RabbitTemplate bean: " +
                                    Arrays.toString(beanNames) +
                                    "Please define a RabbitTemplate bean with this name, " +
                                    "or configure 'outbox.sender.bean-name' property"
                    );
                }
                beanName = beanNames[0];
            }
            if (!context.containsBean(beanName)) {
                throw new IllegalArgumentException(
                        "Cannot create OutboxSender: RabbitTemplate bean '" + beanName + "' not found. " +
                                "Please define a RabbitTemplate bean with this name, " +
                                "or configure 'outbox.sender.bean-name' property"
                );
            }
            properties.setBeanName(beanName);
            rabbitTemplate = context.getBean(beanName, RabbitTemplate.class);
            if (!rabbitTemplate.isMandatoryFor(new Message(Boolean.FALSE.toString().getBytes(StandardCharsets.UTF_8)))) {
                log.error("RabbitTemplate '{}' mandatory flag is false. " +
                        "ReturnedMessage will not be received. You should set mandatory=true for at-least-once", beanName);
            }
            return new RabbitMqOutboxSender(rabbitTemplate, properties.getEmergencyTimeout().toSeconds());
        }
    }
}
