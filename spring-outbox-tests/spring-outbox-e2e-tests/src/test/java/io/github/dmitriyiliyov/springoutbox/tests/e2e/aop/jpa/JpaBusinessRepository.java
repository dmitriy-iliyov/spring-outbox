package io.github.dmitriyiliyov.springoutbox.tests.e2e.aop.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaBusinessRepository extends JpaRepository<BusinessEntity, UUID> { }
