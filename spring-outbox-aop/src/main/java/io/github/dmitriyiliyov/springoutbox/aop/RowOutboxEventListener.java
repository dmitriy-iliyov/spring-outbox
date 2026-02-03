package io.github.dmitriyiliyov.springoutbox.aop;

import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxPublisher;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


public final class RowOutboxEventListener {

    private final OutboxPublisher publisher;

    public RowOutboxEventListener(OutboxPublisher publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void publishEvent(RowOutboxEvent rowEvent) {
        publisher.publish(rowEvent.eventType(), rowEvent.event());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void publishEvents(RowOutboxEvents rowEvent) {
        publisher.publish(rowEvent.eventType(), rowEvent.events());
    }
}
