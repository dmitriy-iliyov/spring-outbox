package io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared;

import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaConsumerBusinessRepository extends JpaRepository<BusinessEntity, UUID> { }
