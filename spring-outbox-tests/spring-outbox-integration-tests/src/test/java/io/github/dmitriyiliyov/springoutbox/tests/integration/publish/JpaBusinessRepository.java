package io.github.dmitriyiliyov.springoutbox.tests.integration.publish;

import io.github.dmitriyiliyov.springoutbox.tests.integration.domain.BusinessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaBusinessRepository extends JpaRepository<BusinessEntity, UUID> { }
