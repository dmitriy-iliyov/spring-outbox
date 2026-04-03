package io.github.dmitriyiliyov.springoutbox.tests.e2e.publish;

import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.domain.BusinessEntity;

import java.util.List;

public interface BusinessRepository {
    BusinessEntity save(BusinessEntity entity);

    List<BusinessEntity> saveAll(List<BusinessEntity> entities);
}
