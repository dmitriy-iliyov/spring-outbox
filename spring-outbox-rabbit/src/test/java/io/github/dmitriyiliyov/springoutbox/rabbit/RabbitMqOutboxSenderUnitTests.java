package io.github.dmitriyiliyov.springoutbox.rabbit;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.SenderResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RabbitMqOutboxSenderUnitTests {

    private static final String EXCHANGE = "test-exchange";
    private static final long TIMEOUT_SECONDS = 5;

    private static ExecutorService executor;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private Channel channel;

    private RabbitMqOutboxSender rabbitMqOutboxSender;

    @BeforeAll
    static void beforeAll() {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterAll
    static void afterAll() {
        executor.shutdown();
    }

    @BeforeEach
    void setUp() {
        rabbitMqOutboxSender = new RabbitMqOutboxSender(rabbitTemplate, TIMEOUT_SECONDS);

        doAnswer(invocation -> {
            ChannelCallback<Void> callback = invocation.getArgument(0);
            return callback.doInRabbit(channel);
        }).when(rabbitTemplate).execute(any(ChannelCallback.class));
    }

    @Test
    @DisplayName("UT sendEvents(), when events is null, should return empty sender result")
    void sendEvents_withNullEvents_shouldReturnEmptyResult() {
        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, null);

        assertThat(result.processedIds()).isEmpty();
        assertThat(result.failedIds()).isEmpty();
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("UT sendEvents(), when events is empty, should return empty sender result")
    void sendEvents_withEmptyEvents_shouldReturnEmptyResult() {
        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, Collections.emptyList());

        assertThat(result.processedIds()).isEmpty();
        assertThat(result.failedIds()).isEmpty();
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("UT sendEvents(), when all events are acked individually, should return all in processedIds")
    void sendEvents_shouldSucceedWhenAllEventsAreAckedIndividually() throws Exception {
        OutboxEvent event1 = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1", Instant.now());
        OutboxEvent event2 = new OutboxEvent(UUID.randomUUID(), "type2", null, "payload2", Instant.now());
        List<OutboxEvent> events = List.of(event1, event2);

        when(channel.getNextPublishSeqNo()).thenReturn(1L, 2L);

        doAnswer(invocation -> {
            ConfirmListener listener = invocation.getArgument(0);
            executor.submit(() -> {
                try {
                    Thread.sleep(100);
                    listener.handleAck(1L, false);
                    listener.handleAck(2L, false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        }).when(channel).addConfirmListener(any(ConfirmListener.class));

        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, events);

        verify(channel, times(2)).basicPublish(eq(EXCHANGE), anyString(), anyBoolean(), any(AMQP.BasicProperties.class), any(byte[].class));
        assertThat(result.processedIds()).containsExactlyInAnyOrder(event1.getId(), event2.getId());
        assertThat(result.failedIds()).isEmpty();
    }

    @Test
    @DisplayName("UT sendEvents(), when all events are acked with multiple flag, should return all in processedIds")
    void sendEvents_shouldSucceedWhenAllEventsAreAckedMultiple() throws Exception {
        OutboxEvent event1 = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1", Instant.now());
        OutboxEvent event2 = new OutboxEvent(UUID.randomUUID(), "type2", null, "payload2", Instant.now());
        List<OutboxEvent> events = List.of(event1, event2);

        when(channel.getNextPublishSeqNo()).thenReturn(1L, 2L);

        doAnswer(invocation -> {
            ConfirmListener listener = invocation.getArgument(0);
            executor.submit(() -> {
                try {
                    Thread.sleep(100);
                    listener.handleAck(2L, true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        }).when(channel).addConfirmListener(any(ConfirmListener.class));

        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, events);

        assertThat(result.processedIds()).containsExactlyInAnyOrder(event1.getId(), event2.getId());
        assertThat(result.failedIds()).isEmpty();
    }

    @Test
    @DisplayName("UT sendEvents(), when all events are nacked individually, should return all in failedIds")
    void sendEvents_shouldFailWhenAllEventsAreNackedIndividually() throws Exception {
        OutboxEvent event1 = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1", Instant.now());
        OutboxEvent event2 = new OutboxEvent(UUID.randomUUID(), "type2", null, "payload2", Instant.now());
        List<OutboxEvent> events = List.of(event1, event2);

        when(channel.getNextPublishSeqNo()).thenReturn(1L, 2L);

        doAnswer(invocation -> {
            ConfirmListener listener = invocation.getArgument(0);
            executor.submit(() -> {
                try {
                    Thread.sleep(100);
                    listener.handleNack(1L, false);
                    listener.handleNack(2L, false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        }).when(channel).addConfirmListener(any(ConfirmListener.class));

        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, events);

        assertThat(result.failedIds()).containsExactlyInAnyOrder(event1.getId(), event2.getId());
        assertThat(result.processedIds()).isEmpty();
    }

    @Test
    @DisplayName("UT sendEvents(), when all events are nacked with multiple flag, should return all in failedIds")
    void sendEvents_shouldFailWhenAllEventsAreNackedMultiple() throws Exception {
        OutboxEvent event1 = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1", Instant.now());
        OutboxEvent event2 = new OutboxEvent(UUID.randomUUID(), "type2", null, "payload2", Instant.now());
        List<OutboxEvent> events = List.of(event1, event2);

        when(channel.getNextPublishSeqNo()).thenReturn(1L, 2L);

        doAnswer(invocation -> {
            ConfirmListener listener = invocation.getArgument(0);
            executor.submit(() -> {
                try {
                    Thread.sleep(100);
                    listener.handleNack(2L, true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        }).when(channel).addConfirmListener(any(ConfirmListener.class));

        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, events);

        assertThat(result.failedIds()).containsExactlyInAnyOrder(event1.getId(), event2.getId());
        assertThat(result.processedIds()).isEmpty();
    }

    @Test
    @DisplayName("UT sendEvents(), when some events acked and some nacked, should split into processedIds and failedIds")
    void sendEvents_shouldHandleMixOfAcksAndNacks() throws Exception {
        OutboxEvent event1 = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1", Instant.now());
        OutboxEvent event2 = new OutboxEvent(UUID.randomUUID(), "type2", null, "payload2", Instant.now());
        List<OutboxEvent> events = List.of(event1, event2);

        when(channel.getNextPublishSeqNo()).thenReturn(1L, 2L);

        doAnswer(invocation -> {
            ConfirmListener listener = invocation.getArgument(0);
            executor.submit(() -> {
                try {
                    Thread.sleep(100);
                    listener.handleAck(1L, false);
                    listener.handleNack(2L, false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        }).when(channel).addConfirmListener(any(ConfirmListener.class));

        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, events);

        assertThat(result.processedIds()).containsExactly(event1.getId());
        assertThat(result.failedIds()).containsExactly(event2.getId());
    }

    @Test
    @DisplayName("UT sendEvents(), when publish throws exception for one event, should mark that event as failed")
    void sendEvents_shouldMarkEventAsFailedWhenPublishThrowsException() throws Exception {
        OutboxEvent event1 = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1", Instant.now());
        OutboxEvent event2 = new OutboxEvent(UUID.randomUUID(), "type2", null, "payload2", Instant.now());
        List<OutboxEvent> events = List.of(event1, event2);

        when(channel.getNextPublishSeqNo()).thenReturn(1L, 2L);

        doThrow(new IOException("Publish failed"))
                .when(channel).basicPublish(eq(EXCHANGE), eq(event2.getEventType()), anyBoolean(), any(AMQP.BasicProperties.class), any(byte[].class));

        doAnswer(invocation -> {
            ConfirmListener listener = invocation.getArgument(0);
            executor.submit(() -> {
                try {
                    Thread.sleep(100);
                    listener.handleAck(1L, false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        }).when(channel).addConfirmListener(any(ConfirmListener.class));

        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, events);

        assertThat(result.processedIds()).containsExactly(event1.getId());
        assertThat(result.failedIds()).containsExactly(event2.getId());
    }

    @Test
    @DisplayName("UT sendEvents(), when rabbitTemplate execute throws exception, should mark all events as failed")
    void sendEvents_shouldMarkAllAsFailedWhenExecuteThrowsException() {
        OutboxEvent event1 = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1", Instant.now());
        List<OutboxEvent> events = List.of(event1);

        doThrow(new AmqpException("Connection failed")).when(rabbitTemplate).execute(any(ChannelCallback.class));

        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, events);

        assertThat(result.failedIds()).containsExactlyInAnyOrder(event1.getId());
        assertThat(result.processedIds()).isEmpty();
    }

    @Test
    @DisplayName("UT sendEvents(), when timeout expires before all confirms, should mark unconfirmed events as failed")
    void sendEvents_shouldMarkUnconfirmedAsFailedOnTimeout() throws Exception {
        rabbitMqOutboxSender = new RabbitMqOutboxSender(rabbitTemplate, 1);
        OutboxEvent event1 = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1", Instant.now());
        OutboxEvent event2 = new OutboxEvent(UUID.randomUUID(), "type2", null, "payload2", Instant.now());
        List<OutboxEvent> events = List.of(event1, event2);

        when(channel.getNextPublishSeqNo()).thenReturn(1L, 2L);

        doAnswer(invocation -> {
            ConfirmListener listener = invocation.getArgument(0);
            executor.submit(() -> {
                try {
                    Thread.sleep(100);
                    listener.handleAck(1L, false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        }).when(channel).addConfirmListener(any(ConfirmListener.class));

        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, events);

        assertThat(result.processedIds()).containsExactly(event1.getId());
        assertThat(result.failedIds()).containsExactly(event2.getId());
    }

    @Test
    @DisplayName("UT sendEvents(), when single event is acked, should return it in processedIds")
    void sendEvents_singleEvent_ackedIndividually_shouldSucceed() throws Exception {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1", Instant.now());

        when(channel.getNextPublishSeqNo()).thenReturn(1L);

        doAnswer(invocation -> {
            ConfirmListener listener = invocation.getArgument(0);
            executor.submit(() -> {
                try {
                    Thread.sleep(100);
                    listener.handleAck(1L, false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        }).when(channel).addConfirmListener(any(ConfirmListener.class));

        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, List.of(event));

        assertThat(result.processedIds()).containsExactly(event.getId());
        assertThat(result.failedIds()).isEmpty();
    }

    @Test
    @DisplayName("UT sendEvents(), when same delivery tag is acked twice, should not process it twice")
    void sendEvents_shouldNotProcessSameTagTwice() throws Exception {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1", Instant.now());

        when(channel.getNextPublishSeqNo()).thenReturn(1L);

        doAnswer(invocation -> {
            ConfirmListener listener = invocation.getArgument(0);
            executor.submit(() -> {
                try {
                    Thread.sleep(100);
                    listener.handleAck(1L, false);
                    listener.handleAck(1L, false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        }).when(channel).addConfirmListener(any(ConfirmListener.class));

        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, List.of(event));

        assertThat(result.processedIds()).containsExactly(event.getId());
        assertThat(result.failedIds()).isEmpty();
    }
}