package io.github.dmitriyiliyov.springoutbox.tests.integration.consume.shared;

import io.github.dmitriyiliyov.springoutbox.tests.integration.domain.BusinessEvent;
import io.github.dmitriyiliyov.springoutbox.tests.integration.utils.IdPreparer;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class JdbcConsumerBusinessRepository implements ConsumerBusinessRepository {

    private final JdbcTemplate jdbcTemplate;
    private final IdPreparer idPreparer;

    public JdbcConsumerBusinessRepository(JdbcTemplate jdbcTemplate, IdPreparer idPreparer) {
        this.jdbcTemplate = jdbcTemplate;
        this.idPreparer = idPreparer;
    }

    @Override
    public BusinessEvent save(BusinessEvent event) {
        jdbcTemplate.update(
                "INSERT INTO business_events (verify_id) VALUES (?)",
                idPreparer.prepare(event.verifyId())
        );
        return event;
    }

    @Override
    public List<BusinessEvent> saveAll(List<BusinessEvent> events) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO business_events (verify_id) VALUES (?)",
                events.stream()
                        .map(event -> new Object [] {idPreparer.prepare(event.verifyId())})
                        .toList()
        );
        return events;
    }
}
