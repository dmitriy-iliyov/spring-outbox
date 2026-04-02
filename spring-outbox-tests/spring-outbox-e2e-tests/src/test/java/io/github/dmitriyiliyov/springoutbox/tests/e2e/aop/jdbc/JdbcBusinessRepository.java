package io.github.dmitriyiliyov.springoutbox.tests.e2e.aop.jdbc;

import io.github.dmitriyiliyov.springoutbox.tests.e2e.aop.domain.BusinessRepository;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.aop.jpa.BusinessEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

public class JdbcBusinessRepository implements BusinessRepository {

    public JdbcBusinessRepository(JdbcTemplate jdbcTemplate, IdPreparer idPreparer) {
        this.jdbcTemplate = jdbcTemplate;
        this.idPreparer = idPreparer;
    }

    private final JdbcTemplate jdbcTemplate;
    private final IdPreparer idPreparer;

    @Override
    public BusinessEntity save(BusinessEntity entity) {
        String sql = """
            INSERT INTO business_events (verify_id)
            VALUES (?)
        """;
        jdbcTemplate.update(
                sql,
                idPreparer.prepare(entity.getVerifyId())
        );
        return entity;
    }

    @Override
    public List<BusinessEntity> saveAll(List<BusinessEntity> entities) {
        String sql = """
            INSERT INTO business_events (verify_id)
            VALUES (?)
        """;
        jdbcTemplate.batchUpdate(
                sql,
                entities.stream().map(e -> new Object [] {idPreparer.prepare(e.getVerifyId())}).toList()
        );
        return entities;
    }

    @FunctionalInterface
    public interface IdPreparer {
        Object prepare(UUID id);
    }
}
