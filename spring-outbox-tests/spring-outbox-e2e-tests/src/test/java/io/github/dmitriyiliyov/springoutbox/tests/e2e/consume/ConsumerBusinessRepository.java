package io.github.dmitriyiliyov.springoutbox.tests.e2e.consume;

import io.github.dmitriyiliyov.springoutbox.tests.e2e.domain.BusinessEvent;

import java.util.List;

public interface ConsumerBusinessRepository {
    BusinessEvent save(BusinessEvent event);
    List<BusinessEvent> saveAll(List<BusinessEvent> events);
}
