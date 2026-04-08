package io.github.dmitriyiliyov.springoutbox.tests.e2e.consume;

import io.github.dmitriyiliyov.springoutbox.tests.e2e.domain.BusinessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaConsumerBusinessRepository extends JpaRepository<BusinessEntity, UUID> { }
