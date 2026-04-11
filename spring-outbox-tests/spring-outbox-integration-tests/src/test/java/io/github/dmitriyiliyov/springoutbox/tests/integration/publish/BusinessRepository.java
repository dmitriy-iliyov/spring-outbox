package io.github.dmitriyiliyov.springoutbox.tests.integration.publish;

import io.github.dmitriyiliyov.springoutbox.tests.integration.domain.BusinessEntity;

import java.util.List;

public interface BusinessRepository {
    BusinessEntity save(BusinessEntity entity);

    List<BusinessEntity> saveAll(List<BusinessEntity> entities);
}
