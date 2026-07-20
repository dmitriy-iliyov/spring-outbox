package io.github.dmitriyiliyov.oncebox.tests.integration.publish;

import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEntity;

import java.util.List;

public interface BusinessRepository {
    BusinessEntity save(BusinessEntity entity);

    List<BusinessEntity> saveAll(List<BusinessEntity> entities);
}
