package io.github.dmitriyiliyov.springoutbox.tests.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.config.BrokerFaultControl;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.domain.BusinessEvent;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.domain.E2eEvents;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.RawEventResender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultEventLifeCycleTests extends BaseE2eTests {

    @Autowired
    RawEventResender rawEventResender;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void shouldDeliverSingleEventEndToEnd() {
        BusinessEvent event = publisherService.saveAndPublish(E2eEvents.DEFAULT_EVENT);

        awaitState().untilAsserted(() -> {
            assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1);
            assertThat(outboxRepository.countEventsByStatus(EventStatus.PROCESSED)).isEqualTo(1);
        });

        UUID eventId = outboxRepository.findEventIds().getFirst();
        assertThat(outboxRepository.isConsumed(eventId)).isTrue();
    }

    @Test
    void shouldDeliverEventBatchEndToEnd() {
        List<BusinessEvent> events = publisherService.saveBatchAndPublish(E2eEvents.DEFAULT_EVENT, 50);

        awaitState().untilAsserted(() ->
                assertThat(outboxRepository.countEventsByStatus(EventStatus.PROCESSED)).isEqualTo(50)
        );
        awaitState().untilAsserted(() ->
                events.forEach(event ->
                        assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1)
                )
        );
    }

    @Test
    void shouldDeliverAopPublishedEventEndToEnd() {
        BusinessEvent event = publisherService.saveAndPublishWithAop();

        awaitState().untilAsserted(() -> {
            assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1);
            assertThat(outboxRepository.countEventsByStatus(EventStatus.PROCESSED)).isEqualTo(1);
        });
    }

    @Test
    void shouldNotPublishWhenBusinessTransactionRollsBack() {
        assertThatThrownBy(() -> publisherService.saveAndFail(E2eEvents.DEFAULT_EVENT))
                .isInstanceOf(IllegalStateException.class);

        awaitAtMost(Duration.ofSeconds(5)).during(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(outboxRepository.countEvents()).isZero();
            assertThat(outboxRepository.countConsumedBusiness()).isZero();
        });
    }

    @Test
    void shouldRejectDuplicateDelivery() throws Exception {
        BusinessEvent event = publisherService.saveAndPublish(E2eEvents.DEFAULT_EVENT);

        awaitState().untilAsserted(() ->
                assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1)
        );

        UUID eventId = outboxRepository.findEventIds().getFirst();
        rawEventResender.resend(
                eventId, E2eEvents.DEFAULT_EVENT, BusinessEvent.class.getName(), objectMapper.writeValueAsString(event)
        );

        awaitAtMost(Duration.ofSeconds(10)).during(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1)
        );
    }

    @Test
    void shouldRecoverStuckEventWithoutCountingRetry() throws Exception {
        BusinessEvent event = BusinessEvent.of();
        UUID eventId = outboxRepository.insertEvent(
                E2eEvents.DEFAULT_EVENT,
                EventStatus.IN_PROCESS,
                BusinessEvent.class.getName(),
                objectMapper.writeValueAsString(event),
                Duration.ofHours(1)
        );

        awaitState().untilAsserted(() -> {
            assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1);
            assertThat(outboxRepository.countEventsByStatus(EventStatus.PROCESSED)).isEqualTo(1);
        });
        assertThat(outboxRepository.findRetryCount(eventId)).isZero();
    }

    @Test
    void shouldRetryAfterBrokerRecovery() {
        BrokerFaultControl.stopBroker();
        try {
            BusinessEvent event = publisherService.saveAndPublish(E2eEvents.RETRY_EVENT);

            awaitAtMost(Duration.ofSeconds(60)).untilAsserted(() -> {
                UUID eventId = outboxRepository.findEventIds().getFirst();
                assertThat(outboxRepository.findRetryCount(eventId)).isGreaterThanOrEqualTo(1);
            });
            assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isZero();

            BrokerFaultControl.startBroker();

            awaitAtMost(Duration.ofSeconds(60)).untilAsserted(() -> {
                assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1);
                assertThat(outboxRepository.countEventsByStatus(EventStatus.PROCESSED)).isEqualTo(1);
            });
        } finally {
            BrokerFaultControl.startBroker();
        }
    }

    @Test
    void shouldCleanUpProcessedEvent() {
        BusinessEvent event = publisherService.saveAndPublish(E2eEvents.DEFAULT_EVENT);

        awaitState().untilAsserted(() ->
                assertThat(outboxRepository.countEventsByStatus(EventStatus.PROCESSED)).isEqualTo(1)
        );

        UUID eventId = outboxRepository.findEventIds().getFirst();
        outboxRepository.shiftEventTimestamps(eventId, Duration.ofHours(1));

        awaitState().untilAsserted(() ->
                assertThat(outboxRepository.countEvents()).isZero()
        );
        assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1);
    }

    @Test
    void shouldCleanUpConsumedEventId() {
        publisherService.saveAndPublish(E2eEvents.DEFAULT_EVENT);

        awaitState().untilAsserted(() ->
                assertThat(outboxRepository.countEventsByStatus(EventStatus.PROCESSED)).isEqualTo(1)
        );
        UUID eventId = outboxRepository.findEventIds().getFirst();
        awaitState().untilAsserted(() ->
                assertThat(outboxRepository.isConsumed(eventId)).isTrue()
        );

        outboxRepository.shiftConsumedEventTimestamp(eventId, Duration.ofHours(1));

        awaitState().untilAsserted(() ->
                assertThat(outboxRepository.isConsumed(eventId)).isFalse()
        );
    }
}
