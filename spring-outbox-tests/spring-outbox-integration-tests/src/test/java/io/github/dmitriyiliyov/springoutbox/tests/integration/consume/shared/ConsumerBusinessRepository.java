package io.github.dmitriyiliyov.springoutbox.tests.integration.consume.shared;

import io.github.dmitriyiliyov.springoutbox.tests.integration.domain.BusinessEvent;

import java.util.List;

public interface ConsumerBusinessRepository {
    BusinessEvent save(BusinessEvent event);
    List<BusinessEvent> saveAll(List<BusinessEvent> events);
}
