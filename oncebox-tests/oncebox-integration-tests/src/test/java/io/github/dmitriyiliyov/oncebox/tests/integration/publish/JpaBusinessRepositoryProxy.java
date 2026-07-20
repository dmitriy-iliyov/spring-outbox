package io.github.dmitriyiliyov.oncebox.tests.integration.publish;

import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEntity;
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
