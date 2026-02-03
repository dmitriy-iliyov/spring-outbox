package io.github.dmitriyiliyov.springoutbox.core.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultOutboxEventIdResolveManagerUnitTests {

    @Mock
    private OutboxEventIdResolver<ConsumerRecord<String, ?>> kafkaResolver;

    @Mock
    private OutboxEventIdResolver<Message> amqpMessageResolver;

    @Mock
    private OutboxEventIdResolver<org.springframework.messaging.Message<?>> springMessageResolver;

    @Test
    @DisplayName("UT resolve() when resolver exists should resolve event id")
    void resolve_whenResolverExists_shouldResolveEventId() {
        // given
        UUID expectedId = UUID.randomUUID();
        ConsumerRecord<String, ?> message = new ConsumerRecord<>("topic", 1, 10L, "key", new Object());
        when(kafkaResolver.getSupports()).thenAnswer(invocation -> ConsumerRecord.class);
        when(kafkaResolver.resolve(message)).thenReturn(expectedId);

        DefaultOutboxEventIdResolveManager manager = new DefaultOutboxEventIdResolveManager(
                List.of(kafkaResolver)
        );

        // when
        UUID result = manager.resolve(message);

        // then
        assertThat(result).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("UT resolve() when resolver not found should throw IllegalArgumentException")
    void resolve_whenResolverNotFound_shouldThrowIllegalArgumentException() {
        // given
        Message message = new Message(new byte[0]);

        DefaultOutboxEventIdResolveManager manager = new DefaultOutboxEventIdResolveManager(
                List.of(kafkaResolver)
        );

        // when + then
        assertThatThrownBy(() -> manager.resolve(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported class 'org.springframework.amqp.core.Message'; cannot resolve");
    }

    @Test
    @DisplayName("UT resolve() with multiple resolvers should use correct resolver")
    void resolve_withMultipleResolvers_shouldUseCorrectResolver() {
        // given
        UUID stringId = UUID.randomUUID();
        UUID integerId = UUID.randomUUID();
        ConsumerRecord<String, ?> kafkaMessage = new ConsumerRecord<>("topic", 1, 10L, "key", new Object());
        Message amqpMessage = new Message(new byte[0]);

        when(kafkaResolver.getSupports()).thenAnswer(invocation -> ConsumerRecord.class);
        when(kafkaResolver.resolve(kafkaMessage)).thenReturn(stringId);
        when(amqpMessageResolver.getSupports()).thenAnswer(invocation -> Message.class);
        when(amqpMessageResolver.resolve(amqpMessage)).thenReturn(integerId);

        DefaultOutboxEventIdResolveManager manager = new DefaultOutboxEventIdResolveManager(
                List.of(kafkaResolver, amqpMessageResolver)
        );

        // when
        UUID stringResult = manager.resolve(kafkaMessage);
        UUID integerResult = manager.resolve(amqpMessage);

        // then
        assertThat(stringResult).isEqualTo(stringId);
        assertThat(integerResult).isEqualTo(integerId);
    }

    @Test
    @DisplayName("UT resolve() batch when resolver exists should resolve all event ids")
    void resolveBatch_whenResolverExists_shouldResolveAllEventIds() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        ConsumerRecord<String, ?> msg1 = new ConsumerRecord<>("topic", 1, 10L, "key", new Object());
        ConsumerRecord<String, ?> msg2 = new ConsumerRecord<>("topic", 1, 10L, "key", new Object());
        ConsumerRecord<String, ?> msg3 = new ConsumerRecord<>("topic", 1, 10L, "key", new Object());
        List<ConsumerRecord<String, ?>> messages = List.of(msg1, msg2, msg3);

        when(kafkaResolver.getSupports()).thenAnswer(invocation -> ConsumerRecord.class);
        when(kafkaResolver.resolve(msg1)).thenReturn(id1);
        when(kafkaResolver.resolve(msg2)).thenReturn(id2);
        when(kafkaResolver.resolve(msg3)).thenReturn(id3);

        DefaultOutboxEventIdResolveManager manager = new DefaultOutboxEventIdResolveManager(
                List.of(kafkaResolver)
        );

        // when
        Map<UUID, ConsumerRecord<String, ?>> result = manager.resolve(messages);

        // then
        assertThat(result).hasSize(3);
        assertThat(result).containsEntry(id1, msg1);
        assertThat(result).containsEntry(id2, msg2);
        assertThat(result).containsEntry(id3, msg3);
    }

    @Test
    @DisplayName("UT resolve() batch when empty list should return empty map")
    void resolveBatch_whenEmptyList_shouldReturnEmptyMap() {
        // given
        List<String> messages = List.of();
        when(kafkaResolver.getSupports()).thenAnswer(invocation -> ConsumerRecord.class);

        DefaultOutboxEventIdResolveManager manager = new DefaultOutboxEventIdResolveManager(
                List.of(kafkaResolver)
        );

        // when
        Map<UUID, String> result = manager.resolve(messages);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("UT resolve() batch when resolver not found should throw IllegalArgumentException")
    void resolveBatch_whenResolverNotFound_shouldThrowIllegalArgumentException() {
        // given
        List<String> messages = List.of("msg1", "msg2");
        when(amqpMessageResolver.getSupports()).thenAnswer(invocation -> Message.class);

        DefaultOutboxEventIdResolveManager manager = new DefaultOutboxEventIdResolveManager(
                List.of(amqpMessageResolver)
        );

        // when + then
        assertThatThrownBy(() -> manager.resolve(messages))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported class 'java.lang.String'; cannot resolve");
    }

    @Test
    @DisplayName("UT resolve() batch with single message should resolve correctly")
    void resolveBatch_withSingleMessage_shouldResolveCorrectly() {
        // given
        UUID id = UUID.randomUUID();
        Message message = new Message(new byte[0]);
        List<Message> messages = List.of(message);

        when(amqpMessageResolver.getSupports()).thenAnswer(invocation -> Message.class);
        when(amqpMessageResolver.resolve(message)).thenReturn(id);

        DefaultOutboxEventIdResolveManager manager = new DefaultOutboxEventIdResolveManager(
                List.of(amqpMessageResolver)
        );

        // when
        Map<UUID, Message> result = manager.resolve(messages);

        // then
        assertThat(result).hasSize(1);
        assertThat(result).containsEntry(id, message);
    }

    @Test
    @DisplayName("UT constructor with empty resolvers list should create manager")
    void constructor_withEmptyResolversList_shouldCreateManager() {
        // given
        List<OutboxEventIdResolver<?>> resolvers = List.of();

        // when
        DefaultOutboxEventIdResolveManager manager = new DefaultOutboxEventIdResolveManager(resolvers);

        // then
        assertThat(manager).isNotNull();
        assertThatThrownBy(() -> manager.resolve("test"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}