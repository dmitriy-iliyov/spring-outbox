package io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.manual;

import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxPublisher;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.BusinessRepository;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.domain.BusinessEntity;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.domain.BusinessEvent;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Class for E2E testing {@link io.github.dmitriyiliyov.springoutbox.core.publisher.DefaultOutboxPublisher}
 */
public class ManualBusinessService {

    public static final String EVENT_TYPE = "business-event";
    private final OutboxPublisher publisher;
    private final BusinessRepository repository;

    @FunctionalInterface
    public interface IdPreparer {
        Object prepare(UUID id);
    }

    public ManualBusinessService(OutboxPublisher publisher, BusinessRepository repository) {
        this.publisher = publisher;
        this.repository = repository;
    }

    @Transactional
    public void successSaveEvent(BusinessEvent event) {
        repository.save(new BusinessEntity(event.verifyId()));
        publisher.publish(EVENT_TYPE, event);
    }

    @Transactional
    public void successSaveEvents(List<BusinessEvent> events) {
        repository.saveAll(
                events.stream().map(event -> new BusinessEntity(event.verifyId())).toList()
        );        publisher.publish(EVENT_TYPE, events);
    }

    public void exceptionallyWithoutTransaction(BusinessEvent event) {
        repository.save(new BusinessEntity(event.verifyId()));
        publisher.publish(EVENT_TYPE, event);
    }

    public void exceptionallyWithoutTransaction(List<BusinessEvent> events) {
        repository.saveAll(
                events.stream().map(event -> new BusinessEntity(event.verifyId())).toList()
        );
        publisher.publish(EVENT_TYPE, events);
    }

    @Transactional
    public void exceptionallyInBusinessTransaction(BusinessEvent event) {
        publisher.publish(EVENT_TYPE, event);
        throw new RuntimeException("Business transaction exception");
    }

    @Transactional
    public void exceptionallyInBusinessTransaction(List<BusinessEvent> events) {
        publisher.publish(EVENT_TYPE, events);
        throw new RuntimeException("Business transaction exception");
    }
}
