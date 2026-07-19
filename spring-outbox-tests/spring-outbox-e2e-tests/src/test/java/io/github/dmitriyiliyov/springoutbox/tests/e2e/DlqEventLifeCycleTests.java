package io.github.dmitriyiliyov.springoutbox.tests.e2e;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.config.KafkaContainerSingleton;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.domain.BusinessEvent;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.domain.E2eEvents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DlqEventLifeCycleTests extends BaseE2eTests {

    private static final String DLQ_API = "/api/outbox-dlq/events";

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void shouldMoveFailedEventToDlqAndDeliverAfterManualRetry() {
        KafkaContainerSingleton.stopBroker();

        BusinessEvent event = publisherService.saveAndPublish(E2eEvents.DLQ_EVENT);

        // Retries are exhausted while the broker is down, then the event is transferred to the DLQ
        awaitAtMost(Duration.ofSeconds(60)).untilAsserted(() ->
                assertThat(outboxRepository.countDlqEventsByStatus(DlqStatus.MOVED)).isEqualTo(1)
        );
        assertThat(outboxRepository.countEvents()).isZero();

        KafkaContainerSingleton.startBroker();

        UUID dlqEventId = outboxRepository.findDlqEventIdsByStatus(DlqStatus.MOVED).getFirst();
        ResponseEntity<Void> response = patchStatus(dlqEventId, DlqStatus.TO_RETRY);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // The event returns to the outbox and completes its normal lifecycle
        awaitAtMost(Duration.ofSeconds(60)).untilAsserted(() -> {
            assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1);
            assertThat(outboxRepository.countEventsByStatus(EventStatus.PROCESSED)).isEqualTo(1);
            assertThat(outboxRepository.countDlqEvents()).isZero();
        });
    }

    @Test
    void shouldCleanUpResolvedDlqEvent() {
        KafkaContainerSingleton.stopBroker();

        publisherService.saveAndPublish(E2eEvents.DLQ_EVENT);

        awaitAtMost(Duration.ofSeconds(60)).untilAsserted(() ->
                assertThat(outboxRepository.countDlqEventsByStatus(DlqStatus.MOVED)).isEqualTo(1)
        );

        KafkaContainerSingleton.startBroker();

        UUID dlqEventId = outboxRepository.findDlqEventIdsByStatus(DlqStatus.MOVED).getFirst();
        ResponseEntity<Void> response = patchStatus(dlqEventId, DlqStatus.RESOLVED);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        outboxRepository.shiftDlqEventTimestamps(dlqEventId, Duration.ofHours(1));

        awaitState().untilAsserted(() ->
                assertThat(outboxRepository.countDlqEvents()).isZero()
        );
    }

    @Test
    void shouldManageDlqEventsViaRestApi() {
        UUID firstId = outboxRepository.insertDlqEvent(
                "e2e-dlq-event", DlqStatus.MOVED, BusinessEvent.class.getName(), "{\"verifyId\":\"" + UUID.randomUUID() + "\"}"
        );
        UUID secondId = outboxRepository.insertDlqEvent(
                "e2e-dlq-event", DlqStatus.MOVED, BusinessEvent.class.getName(), "{\"verifyId\":\"" + UUID.randomUUID() + "\"}"
        );

        ResponseEntity<Map<String, Object>> getResponse = restTemplate.exchange(
                DLQ_API + "/" + firstId, HttpMethod.GET, null,
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).containsEntry("id", firstId.toString());

        ResponseEntity<Long> countResponse = restTemplate.getForEntity(
                DLQ_API + "/count?status=MOVED&eventType=e2e-dlq-event", Long.class
        );
        assertThat(countResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(countResponse.getBody()).isEqualTo(2L);

        ResponseEntity<Object[]> batchResponse = restTemplate.getForEntity(
                DLQ_API + "/batch?status=MOVED&batchNumber=0&batchSize=10", Object[].class
        );
        assertThat(batchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(batchResponse.getBody()).hasSize(2);

        restTemplate.delete(DLQ_API + "/" + secondId);
        awaitState().untilAsserted(() ->
                assertThat(outboxRepository.countDlqEvents()).isEqualTo(1)
        );
    }

    private ResponseEntity<Void> patchStatus(UUID dlqEventId, DlqStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(
                DLQ_API + "/" + dlqEventId,
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", status.name()), headers),
                Void.class
        );
    }
}
