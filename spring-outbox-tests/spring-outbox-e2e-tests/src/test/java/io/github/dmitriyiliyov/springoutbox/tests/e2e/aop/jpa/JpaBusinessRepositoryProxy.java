package io.github.dmitriyiliyov.springoutbox.tests.e2e.aop.jpa;

import io.github.dmitriyiliyov.springoutbox.tests.e2e.aop.domain.BusinessRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JpaBusinessRepositoryProxy implements BusinessRepository {

    private final JpaBusinessRepository repository;

    public JpaBusinessRepositoryProxy(JpaBusinessRepository repository) {
        this.repository = repository;
    }

    @Override
    public BusinessEntity save(BusinessEntity entity) {
        return repository.save(entity);
    }

    @Override
    public List<BusinessEntity> saveAll(List<BusinessEntity> entities) {
        return repository.saveAll(entities);
    }
}
