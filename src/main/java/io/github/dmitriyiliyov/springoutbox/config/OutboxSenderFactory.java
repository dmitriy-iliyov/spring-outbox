package io.github.dmitriyiliyov.springoutbox.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.KafkaOutboxSender;
import io.github.dmitriyiliyov.springoutbox.core.OutboxSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

public final class OutboxSenderFactory {

    private static final Logger log = LoggerFactory.getLogger(OutboxSenderFactory.class);
    private static final Map<SenderType, OutboxSenderSupplier> SUPPORTED_BROKERS = Map.of(
            SenderType.KAFKA, new KafkaOutboxSenderSupplier(),
            SenderType.RABBIT_MQ, new RabbitMqOutboxSenderSupplier()
    );

    private OutboxSenderFactory() {}

    public static OutboxSender generate(OutboxProperties.SenderProperties properties,
                                        ApplicationContext context,
                                        ObjectMapper mapper) {
        SenderType type = properties.type();
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
        OutboxSender supply(OutboxProperties.SenderProperties properties,
                            ApplicationContext context,
                            ObjectMapper mapper);
    }

    private static class KafkaOutboxSenderSupplier implements OutboxSenderSupplier {

        @Override
        public OutboxSender supply(OutboxProperties.SenderProperties properties,
                                   ApplicationContext context,
                                   ObjectMapper mapper) {
            String beanName = properties.beanName();
            if (beanName == null || beanName.isEmpty()) {
                log.warn("Sender beanName is null or blank; using default bean name 'outboxKafkaTemplate'");
                beanName = "outboxKafkaTemplate";
            }
            if (!context.containsBean(beanName)) {
                throw new IllegalStateException(
                        "Cannot create OutboxSender: KafkaTemplate bean '" + beanName + "' not found. " +
                                "Please define a KafkaTemplate<String, Object> bean with this name, " +
                                "or configure 'outbox.sender.kafka-template-bean-name' property."
                );
            }
            KafkaTemplate<String, Object> kafkaTemplate = context.getBean(beanName, KafkaTemplate.class);
            Map<String, Object> configs = kafkaTemplate.getProducerFactory().getConfigurationProperties();
            String acks = (String) configs.get("acks");
            if (acks != null && !acks.equals("all")) {
                log.warn("Kafka producer factory is configured without 'acks=all'. Outbox cannot guarantee at-least-once delivery.");
            }
            Boolean idempotence = null;
            Object idempotenceObj = configs.get("enable.idempotence");
            if (idempotenceObj instanceof Boolean) {
                idempotence = (Boolean) idempotenceObj;
            } else if (idempotenceObj instanceof String) {
                idempotence = Boolean.parseBoolean((String) idempotenceObj);
            }
            if (idempotence != null && !idempotence) {
                log.warn("Kafka producer is not idempotent. It is recommended to enable 'enable.idempotence=true' to avoid message duplication.");
            }
            return new KafkaOutboxSender(kafkaTemplate, mapper);
        }
    }

    private static class RabbitMqOutboxSenderSupplier implements OutboxSenderSupplier {

        @Override
        public OutboxSender supply(OutboxProperties.SenderProperties properties,
                                   ApplicationContext context,
                                   ObjectMapper mapper) {
            throw new UnsupportedOperationException(
                    "RabbitMQ OutboxSender is not implemented yet"
            );
        }
    }
}