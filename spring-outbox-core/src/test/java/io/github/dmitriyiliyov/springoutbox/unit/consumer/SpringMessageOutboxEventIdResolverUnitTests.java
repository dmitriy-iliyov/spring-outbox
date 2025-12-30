package io.github.dmitriyiliyov.springoutbox.unit.consumer;

import io.github.dmitriyiliyov.springoutbox.consumer.SpringMessageOutboxEventIdResolver;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringMessageOutboxEventIdResolverUnitTests {

    private final SpringMessageOutboxEventIdResolver resolver = new SpringMessageOutboxEventIdResolver();

    @Test
    @DisplayName("UT resolve() should return UUID from message headers")
    void resolve_shouldReturnUuidFromHeaders() {
        // given
        UUID expectedId = UUID.randomUUID();
        Message<String> message = MessageBuilder.withPayload("test")
                .setHeader(OutboxHeaders.EVENT_ID.getValue(), expectedId)
                .build();

        // when
        UUID result = resolver.resolve(message);

        // then
        assertThat(result).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("UT resolve() should throw IllegalArgumentException if header missing")
    void resolve_shouldThrowExceptionIfHeaderMissing() {
        // given
        Message<String> message = MessageBuilder.withPayload("test").build();

        // when + then
        assertThatThrownBy(() -> resolver.resolve(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Header '%s' not found".formatted(OutboxHeaders.EVENT_ID.getValue()));
    }

    @Test
    @DisplayName("UT getSupports() should return Message.class")
    void getSupports_shouldReturnMessageClass() {
        // when
        Class<?> result = resolver.getSupports();

        // then
        assertThat(result).isEqualTo(Message.class);
    }

    @Test
    @DisplayName("UT resolve() with different payload types should still resolve UUID")
    void resolve_withDifferentPayloadTypes_shouldResolveUuid() {
        // given
        UUID expectedId = UUID.randomUUID();
        Message<Integer> messageInt = MessageBuilder.withPayload(42)
                .setHeader(OutboxHeaders.EVENT_ID.getValue(), expectedId)
                .build();

        Message<Double> messageDouble = MessageBuilder.withPayload(3.14)
                .setHeader(OutboxHeaders.EVENT_ID.getValue(), expectedId)
                .build();

        // when
        UUID resultInt = resolver.resolve(messageInt);
        UUID resultDouble = resolver.resolve(messageDouble);

        // then
        assertThat(resultInt).isEqualTo(expectedId);
        assertThat(resultDouble).isEqualTo(expectedId);
    }
}
