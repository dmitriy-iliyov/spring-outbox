package io.github.dmitriyiliyov.springoutbox.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxHeaders;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.SenderResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = KafkaOutboxSenderIntegrationTests.KafkaTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("kafka-it")
class KafkaOutboxSenderIntegrationTests {

    private static final String TOPIC = "integration-test-topic";

    static final KafkaContainer kafka;

    static {
        kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
        kafka.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");
        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add("spring.kafka.consumer.value-deserializer",
                () -> "org.springframework.kafka.support.serializer.JsonDeserializer");
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaMessageReceiver receiver;

    private KafkaOutboxSender sender;

    @BeforeEach
    void setUp() {
        receiver.clear();
        sender = new KafkaOutboxSender(kafkaTemplate, 5L, objectMapper);
    }

    @Test
    @DisplayName("IT sendEvents() should successfully send valid event to Kafka and return it in processedIds")
    void sendEvents_validEvent_successfullySentToKafka() throws InterruptedException {
        UUID eventId = UUID.randomUUID();
        String payloadJson = "{\"testField\":\"Hello Kafka\"}";
        String payloadType = DummyPayload.class.getName();

        OutboxEvent event = createEvent(eventId, "TEST_EVENT", payloadType, payloadJson);

        SenderResult result = sender.sendEvents(TOPIC, List.of(event));

        assertThat(result.processedIds()).containsExactly(eventId);
        assertThat(result.failedIds()).isEmpty();

        ConsumerRecord<String, Object> received = receiver.poll(10, TimeUnit.SECONDS);
        assertThat(received).isNotNull();

        byte[] eventIdHeader = received.headers().lastHeader(OutboxHeaders.EVENT_ID.getValue()).value();
        assertThat(new String(eventIdHeader)).isEqualTo(eventId.toString());

        byte[] eventTypeHeader = received.headers().lastHeader(OutboxHeaders.EVENT_TYPE.getValue()).value();
        assertThat(new String(eventTypeHeader)).isEqualTo("TEST_EVENT");

        assertThat(received.value()).isInstanceOf(DummyPayload.class);
        DummyPayload receivedPayload = (DummyPayload) received.value();
        assertThat(receivedPayload.getTestField()).isEqualTo("Hello Kafka");
    }

    @Test
    @DisplayName("IT sendEvents() with invalid payload class should fail and return in failedIds")
    void sendEvents_invalidPayloadClass_returnsFailed() {
        UUID eventId = UUID.randomUUID();
        String payloadJson = "{\"testField\":\"Hello\"}";
        String payloadType = "io.github.unknown.NonExistentClass";

        OutboxEvent event = createEvent(eventId, "TEST_EVENT", payloadType, payloadJson);

        SenderResult result = sender.sendEvents(TOPIC, List.of(event));

        assertThat(result.processedIds()).isEmpty();
        assertThat(result.failedIds()).containsExactly(eventId);
    }

    @Test
    @DisplayName("IT sendEvents() with invalid JSON should fail and return in failedIds")
    void sendEvents_invalidJson_returnsFailed() {
        UUID eventId = UUID.randomUUID();
        String invalidJson = "{broken_json:";
        String payloadType = DummyPayload.class.getName();

        OutboxEvent event = createEvent(eventId, "TEST_EVENT", payloadType, invalidJson);

        SenderResult result = sender.sendEvents(TOPIC, List.of(event));

        assertThat(result.processedIds()).isEmpty();
        assertThat(result.failedIds()).containsExactly(eventId);
    }

    private OutboxEvent createEvent(UUID id, String eventType, String payloadType, String payload) {
        Instant now = Instant.now();
        return new OutboxEvent(id, EventStatus.PENDING, eventType, payloadType, payload, 0, now, now, now);
    }

    @TestConfiguration
    @EnableKafka
    static class KafkaTestConfig {

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public KafkaMessageReceiver kafkaMessageReceiver() {
            return new KafkaMessageReceiver();
        }
    }

    static class KafkaMessageReceiver {
        private final BlockingQueue<ConsumerRecord<String, Object>> records = new LinkedBlockingQueue<>();

        @KafkaListener(topics = TOPIC)
        public void listen(ConsumerRecord<String, Object> record) {
            records.add(record);
        }

        public ConsumerRecord<String, Object> poll(long timeout, TimeUnit unit) throws InterruptedException {
            return records.poll(timeout, unit);
        }

        public void clear() {
            records.clear();
        }
    }

    public static class DummyPayload {
        private String testField;

        public DummyPayload() {}

        public String getTestField() { return testField; }
        public void setTestField(String testField) { this.testField = testField; }
    }
}
