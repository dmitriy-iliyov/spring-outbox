package io.github.dmitriyiliyov.oncebox.tests.e2e.publish;

import java.util.UUID;

/**
 * Broker-agnostic way to re-publish an already-delivered event with the same outbox event id,
 * simulating a broker redelivery so the idempotent consumer can be exercised.
 */
public interface RawEventResender {

    void resend(UUID eventId, String eventType, String payloadType, String payloadJson);
}
