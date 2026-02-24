package io.github.dmitriyiliyov.springoutbox.rabbit;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.SenderResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

    @Captor
    private ArgumentCaptor<ChannelCallback<Void>> channelCallbackCaptor;

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
            channelCallbackCaptor.getValue().doInRabbit(channel);
            return null;
        }).when(rabbitTemplate).execute(channelCallbackCaptor.capture());
    }

    @Test
    void sendEvents_withNullEvents_shouldReturnEmptyResult() {
        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, null);

        assertThat(result.processedIds()).isEmpty();
        assertThat(result.failedIds()).isEmpty();
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void sendEvents_withEmptyEvents_shouldReturnEmptyResult() {
        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, Collections.emptyList());

        assertThat(result.processedIds()).isEmpty();
        assertThat(result.failedIds()).isEmpty();
        verifyNoInteractions(rabbitTemplate);
    }

//    @Test
//    void sendEvents_shouldSucceedWhenAllEventsAreAckedIndividually() throws Exception {
//        OutboxEvent event1 = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1");
//        OutboxEvent event2 = new OutboxEvent(UUID.randomUUID(), "type2", null, "payload2");
//        List<OutboxEvent> events = List.of(event1, event2);
//
//        when(channel.getNextPublishSeqNo()).thenReturn(1L, 2L);
//        doAnswer(invocation -> {
//            ConfirmListener listener = invocation.getArgument(0);
//            executor.submit(() -> {
//                try {
//                    listener.handleAck(1L, false);
//                    listener.handleAck(2L, false);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//            return null;
//        }).when(channel).addConfirmListener(any(ConfirmListener.class));
//
//        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, events);
//
//        verify(channel, times(2)).basicPublish(eq(EXCHANGE), anyString(), anyBoolean(), any(AMQP.BasicProperties.class), any(byte[].class));
//        assertThat(result.processedIds()).containsExactlyInAnyOrder(event1.getId(), event2.getId());
//        assertThat(result.failedIds()).isEmpty();
//    }

//    @Test
//    void sendEvents_shouldSucceedWhenAllEventsAreAckedMultiple() throws Exception {
//        OutboxEvent event1 = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1");
//        OutboxEvent event2 = new OutboxEvent(UUID.randomUUID(), "type2", null, "payload2");
//        List<OutboxEvent> events = List.of(event1, event2);
//
//        when(channel.getNextPublishSeqNo()).thenReturn(1L, 2L);
//        doAnswer(invocation -> {
//            ConfirmListener listener = invocation.getArgument(0);
//            executor.submit(() -> {
//                try {
//                    listener.handleAck(2L, true);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//            return null;
//        }).when(channel).addConfirmListener(any(ConfirmListener.class));
//
//        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, events);
//
//        assertThat(result.processedIds()).containsExactlyInAnyOrder(event1.getId(), event2.getId());
//        assertThat(result.failedIds()).isEmpty();
//    }

    @Test
    void sendEvents_shouldFailWhenAllEventsAreNackedIndividually() throws Exception {
        OutboxEvent event1 = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1");
        OutboxEvent event2 = new OutboxEvent(UUID.randomUUID(), "type2", null, "payload2");
        List<OutboxEvent> events = List.of(event1, event2);

        when(channel.getNextPublishSeqNo()).thenReturn(1L, 2L);
        doAnswer(invocation -> {
            ConfirmListener listener = invocation.getArgument(0);
            executor.submit(() -> {
                try {
                    listener.handleNack(1L, false);
                    listener.handleNack(2L, false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        }).when(channel).addConfirmListener(any(ConfirmListener.class));

        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, events);

        assertThat(result.failedIds()).containsExactlyInAnyOrder(event1.getId(), event2.getId());
        assertThat(result.processedIds()).isEmpty();
    }

//    @Test
//    void sendEvents_shouldHandleMixOfAcksAndNacks() throws Exception {
//        OutboxEvent event1 = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1");
//        OutboxEvent event2 = new OutboxEvent(UUID.randomUUID(), "type2", null, "payload2");
//        List<OutboxEvent> events = List.of(event1, event2);
//
//        when(channel.getNextPublishSeqNo()).thenReturn(1L, 2L);
//        doAnswer(invocation -> {
//            ConfirmListener listener = invocation.getArgument(0);
//            executor.submit(() -> {
//                try {
//                    listener.handleAck(1L, false);
//                    listener.handleNack(2L, false);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//            return null;
//        }).when(channel).addConfirmListener(any(ConfirmListener.class));
//
//        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, events);
//
//        assertThat(result.processedIds()).containsExactly(event1.getId());
//        assertThat(result.failedIds()).containsExactly(event2.getId());
//    }

//    @Test
//    void sendEvents_shouldMarkEventAsFailedWhenPublishThrowsException() throws Exception {
//        OutboxEvent event1 = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1");
//        OutboxEvent event2 = new OutboxEvent(UUID.randomUUID(), "type2", null, "payload2");
//        List<OutboxEvent> events = List.of(event1, event2);
//
//        when(channel.getNextPublishSeqNo()).thenReturn(1L, 2L);
//        doThrow(new IOException("Publish failed"))
//                .when(channel).basicPublish(eq(EXCHANGE), eq(event2.getEventType()), anyBoolean(), any(AMQP.BasicProperties.class), any(byte[].class));
//
//        doAnswer(invocation -> {
//            ConfirmListener listener = invocation.getArgument(0);
//            executor.submit(() -> {
//                try {
//                    listener.handleAck(1L, false);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//            return null;
//        }).when(channel).addConfirmListener(any(ConfirmListener.class));
//
//        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, events);
//
//        assertThat(result.processedIds()).containsExactly(event1.getId());
//        assertThat(result.failedIds()).containsExactly(event2.getId());
//    }

    @Test
    void sendEvents_shouldMarkAllAsFailedWhenExecuteThrowsException() {
        OutboxEvent event1 = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1");
        List<OutboxEvent> events = List.of(event1);

        doThrow(new AmqpException("Connection failed")).when(rabbitTemplate).execute(any(ChannelCallback.class));

        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, events);

        assertThat(result.failedIds()).containsExactlyInAnyOrder(event1.getId());
        assertThat(result.processedIds()).isEmpty();
    }

//    @Test
//    void sendEvents_shouldMarkUnconfirmedAsFailedOnTimeout() throws Exception {
//        rabbitMqOutboxSender = new RabbitMqOutboxSender(rabbitTemplate, 1); // 1-second timeout
//        OutboxEvent event1 = new OutboxEvent(UUID.randomUUID(), "type1", null, "payload1");
//        OutboxEvent event2 = new OutboxEvent(UUID.randomUUID(), "type2", null, "payload2");
//        List<OutboxEvent> events = List.of(event1, event2);
//
//        when(channel.getNextPublishSeqNo()).thenReturn(1L, 2L);
//        doAnswer(invocation -> {
//            ConfirmListener listener = invocation.getArgument(0);
//            executor.submit(() -> {
//                try {
//                    listener.handleAck(1L, false);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//            return null;
//        }).when(channel).addConfirmListener(any(ConfirmListener.class));
//
//        SenderResult result = rabbitMqOutboxSender.sendEvents(EXCHANGE, events);
//
//        assertThat(result.processedIds()).containsExactly(event1.getId());
//        assertThat(result.failedIds()).containsExactly(event2.getId());
//    }
}
