package io.github.dmitriyiliyov.springoutbox.unit.consumer;

import io.github.dmitriyiliyov.springoutbox.consumer.DefaultOutboxEventIdResolveManager;
import io.github.dmitriyiliyov.springoutbox.consumer.OutboxEventIdResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultOutboxEventIdResolveManagerUnitTests {

    @Mock
    private OutboxEventIdResolver<String> stringResolver;

    @Mock
    private OutboxEventIdResolver<Integer> integerResolver;

    @Test
    @DisplayName("UT resolve() when resolver exists should resolve event id")
    void resolve_whenResolverExists_shouldResolveEventId() {
        // given
        UUID expectedId = UUID.randomUUID();
        String message = "test-message";
        when(stringResolver.getSupports()).thenAnswer(invocation -> String.class);
        when(stringResolver.resolve(message)).thenReturn(expectedId);

        DefaultOutboxEventIdResolveManager manager = new DefaultOutboxEventIdResolveManager(
                List.of(stringResolver)
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
        String message = "test-message";
        when(integerResolver.getSupports()).thenAnswer(invocation -> Integer.class);

        DefaultOutboxEventIdResolveManager manager = new DefaultOutboxEventIdResolveManager(
                List.of(integerResolver)
        );

        // when + then
        assertThatThrownBy(() -> manager.resolve(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported class 'java.lang.String'; cannot resolve");
    }

    @Test
    @DisplayName("UT resolve() with multiple resolvers should use correct resolver")
    void resolve_withMultipleResolvers_shouldUseCorrectResolver() {
        // given
        UUID stringId = UUID.randomUUID();
        UUID integerId = UUID.randomUUID();
        String stringMessage = "test";
        Integer integerMessage = 42;

        when(stringResolver.getSupports()).thenAnswer(invocation -> String.class);
        when(stringResolver.resolve(stringMessage)).thenReturn(stringId);
        when(integerResolver.getSupports()).thenAnswer(invocation -> Integer.class);
        when(integerResolver.resolve(integerMessage)).thenReturn(integerId);

        DefaultOutboxEventIdResolveManager manager = new DefaultOutboxEventIdResolveManager(
                List.of(stringResolver, integerResolver)
        );

        // when
        UUID stringResult = manager.resolve(stringMessage);
        UUID integerResult = manager.resolve(integerMessage);

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
        String msg1 = "msg1";
        String msg2 = "msg2";
        String msg3 = "msg3";
        List<String> messages = List.of(msg1, msg2, msg3);

        when(stringResolver.getSupports()).thenAnswer(invocation -> String.class);
        when(stringResolver.resolve(msg1)).thenReturn(id1);
        when(stringResolver.resolve(msg2)).thenReturn(id2);
        when(stringResolver.resolve(msg3)).thenReturn(id3);

        DefaultOutboxEventIdResolveManager manager = new DefaultOutboxEventIdResolveManager(
                List.of(stringResolver)
        );

        // when
        Map<UUID, String> result = manager.resolve(messages);

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
        when(stringResolver.getSupports()).thenAnswer(invocation -> String.class);

        DefaultOutboxEventIdResolveManager manager = new DefaultOutboxEventIdResolveManager(
                List.of(stringResolver)
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
        when(integerResolver.getSupports()).thenAnswer(invocation -> Integer.class);

        DefaultOutboxEventIdResolveManager manager = new DefaultOutboxEventIdResolveManager(
                List.of(integerResolver)
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
        String message = "single-msg";
        List<String> messages = List.of(message);

        when(stringResolver.getSupports()).thenAnswer(invocation -> String.class);
        when(stringResolver.resolve(message)).thenReturn(id);

        DefaultOutboxEventIdResolveManager manager = new DefaultOutboxEventIdResolveManager(
                List.of(stringResolver)
        );

        // when
        Map<UUID, String> result = manager.resolve(messages);

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