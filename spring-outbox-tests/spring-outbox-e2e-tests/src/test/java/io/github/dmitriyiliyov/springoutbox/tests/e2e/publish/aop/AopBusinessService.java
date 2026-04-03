package io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.aop;

import io.github.dmitriyiliyov.springoutbox.aop.OutboxPublish;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.BusinessRepository;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.domain.BusinessEntity;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.domain.BusinessEvent;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class AopBusinessService {

    public static final String EVENT_TYPE = "business-event";
    private final BusinessRepository repository;

    public AopBusinessService(BusinessRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE, payload = "#event")
    public void successSaveEvent(BusinessEvent event) {
        repository.save(new BusinessEntity(event.verifyId()));
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE, payload = "#events")
    public void successSaveEvents(List<BusinessEvent> events) {
        repository.saveAll(
                events.stream().map(event -> new BusinessEntity(event.verifyId())).toList()
        );
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE)
    public BusinessEvent successSaveReturnedEvent(BusinessEvent event) {
        BusinessEntity entity = repository.save(new BusinessEntity(event.verifyId()));
        return BusinessEvent.of(entity.getVerifyId());
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE)
    public List<BusinessEvent> successSaveReturnedEvents(List<BusinessEvent> events) {
        List<BusinessEntity> entities = repository.saveAll(
                events.stream().map(event -> new BusinessEntity(event.verifyId())).toList()
        );
        return entities.stream().map(entity -> BusinessEvent.of(entity.getVerifyId())).toList();
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE, payload = "#event")
    public void exceptionallyInBusinessTransaction(BusinessEvent event) {
        throw new RuntimeException("Business transaction exception");
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE, payload = "#events")
    public void exceptionallyInBusinessTransaction(List<BusinessEvent> events) {
        throw new RuntimeException("Business transaction exception");
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE, payload = "#event")
    public void exceptionallyInBusinessTransactionWithReturnedEvent(BusinessEvent event) {
        throw new RuntimeException("Business transaction exception");
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE, payload = "#events")
    public void exceptionallyInBusinessTransactionWithReturnedEvents(List<BusinessEvent> events) {
        throw new RuntimeException("Business transaction exception");
    }
}
