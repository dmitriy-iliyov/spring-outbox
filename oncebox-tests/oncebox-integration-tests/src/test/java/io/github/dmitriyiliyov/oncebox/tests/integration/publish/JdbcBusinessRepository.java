package io.github.dmitriyiliyov.oncebox.tests.integration.publish;

import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEntity;
import io.github.dmitriyiliyov.oncebox.tests.integration.utils.IdPreparer;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

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
}
