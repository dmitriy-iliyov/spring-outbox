package io.github.dmitriyiliyov.springoutbox.tests.e2e.publish;

import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.domain.BusinessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaBusinessRepository extends JpaRepository<BusinessEntity, UUID> { }
