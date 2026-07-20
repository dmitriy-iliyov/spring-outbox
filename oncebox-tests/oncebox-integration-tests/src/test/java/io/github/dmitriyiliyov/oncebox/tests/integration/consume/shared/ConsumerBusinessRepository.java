package io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared;

import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEvent;

import java.util.List;

public interface ConsumerBusinessRepository {
    BusinessEvent save(BusinessEvent event);
    List<BusinessEvent> saveAll(List<BusinessEvent> events);
}
