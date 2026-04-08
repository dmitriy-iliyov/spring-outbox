package io.github.dmitriyiliyov.springoutbox.tests.e2e.consume;

import io.github.dmitriyiliyov.springoutbox.tests.e2e.domain.BusinessEntity;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.domain.BusinessEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JpaConsumerBusinessRepositoryProxy implements ConsumerBusinessRepository {

    public final JpaConsumerBusinessRepository repository;

    public JpaConsumerBusinessRepositoryProxy(JpaConsumerBusinessRepository repository) {
        this.repository = repository;
    }

    @Override
    public BusinessEvent save(BusinessEvent event) {
        repository.save(new BusinessEntity(event.verifyId()));
        return event;
    }

    @Override
    public List<BusinessEvent> saveAll(List<BusinessEvent> events) {
        repository.saveAll(events.stream().map(event -> new BusinessEntity(event.verifyId())).toList());
        return events;
    }
}
