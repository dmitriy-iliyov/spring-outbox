package io.github.dmitriyiliyov.springoutbox.rabbit;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RabbitMqOutboxEventIdResolverUnitTests {

    private final RabbitMqOutboxEventIdResolver resolver = new RabbitMqOutboxEventIdResolver();

    @Test
    @DisplayName("UT resolve() should return UUID from headers if UUID type")
    void resolve_shouldReturnUuidWhenHeaderIsUuid() {
        // given
        UUID expectedId = UUID.randomUUID();
        Map<String, Object> headers = new HashMap<>();
        headers.put(OutboxHeaders.EVENT_ID.getValue(), expectedId);
        MessageProperties props = new MessageProperties();
        props.getHeaders().putAll(headers);
        Message message = new Message("payload".getBytes(StandardCharsets.UTF_8), props);

        // when
        UUID result = resolver.resolve(message);

        // then
        assertThat(result).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("UT resolve() should parse UUID from String header")
    void resolve_shouldParseUuidFromStringHeader() {
        // given
        UUID expectedId = UUID.randomUUID();
        String uuidString = expectedId.toString();
        Map<String, Object> headers = new HashMap<>();
        headers.put(OutboxHeaders.EVENT_ID.getValue(), uuidString);
        MessageProperties props = new MessageProperties();
        props.getHeaders().putAll(headers);
        Message message = new Message("payload".getBytes(StandardCharsets.UTF_8), props);

        // when
        UUID result = resolver.resolve(message);

        // then
        assertThat(result).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("UT resolve() should throw if header missing")
    void resolve_shouldThrowIfHeaderMissing() {
        // given
        MessageProperties props = new MessageProperties();
        Message message = new Message("payload".getBytes(StandardCharsets.UTF_8), props);

        // when + then
        assertThatThrownBy(() -> resolver.resolve(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Header '%s' not found".formatted(OutboxHeaders.EVENT_ID.getValue()));
    }

    @Test
    @DisplayName("UT resolve() should throw if header has invalid type")
    void resolve_shouldThrowIfHeaderHasInvalidType() {
        // given
        Map<String, Object> headers = new HashMap<>();
        headers.put(OutboxHeaders.EVENT_ID.getValue(), 12345); // Integer, invalid
        MessageProperties props = new MessageProperties();
        props.getHeaders().putAll(headers);
        Message message = new Message("payload".getBytes(StandardCharsets.UTF_8), props);

        // when + then
        assertThatThrownBy(() -> resolver.resolve(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Header '%s' has invalid type".formatted(OutboxHeaders.EVENT_ID.getValue()));
    }

    @Test
    @DisplayName("UT getSupports() should return Message.class")
    void getSupports_shouldReturnMessageClass() {
        // when
        Class<?> result = resolver.getSupports();

        // then
        assertThat(result).isEqualTo(Message.class);
    }
}
