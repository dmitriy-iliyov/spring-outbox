package io.github.dmitriyiliyov.springoutbox.aop;

import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RawOutboxEventListenerTest {

    @Mock
    private OutboxPublisher publisher;

    @InjectMocks
    private RawOutboxEventListener eventListener;

    @Test
    @DisplayName("UT publishEvent() should call publisher with single event")
    void publishEvent_shouldCallPublisherWithSingleEvent() {
        // given
        Object eventPayload = new Object();
        RawOutboxEvent rowEvent = new RawOutboxEvent("test-event", eventPayload);

        // when
        eventListener.publishEvent(rowEvent);

        // then
        verify(publisher).publish("test-event", eventPayload);
    }

    @Test
    @DisplayName("UT publishEvents() should call publisher with list of events")
    void publishEvents_shouldCallPublisherWithListOfEvents() {
        // given
        List<Object> eventPayloads = List.of(new Object(), new Object());
        RawOutboxEvents rowEvents = new RawOutboxEvents("test-event-batch", eventPayloads);

        // when
        eventListener.publishEvents(rowEvents);

        // then
        verify(publisher).publish("test-event-batch", eventPayloads);
    }
}
