package io.github.dmitriyiliyov.springoutbox.tests.e2e.aop.domain;

import io.github.dmitriyiliyov.springoutbox.tests.e2e.aop.jpa.BusinessEntity;

import java.util.List;

public interface BusinessRepository {
    BusinessEntity save(BusinessEntity entity);

    List<BusinessEntity> saveAll(List<BusinessEntity> entities);
}
