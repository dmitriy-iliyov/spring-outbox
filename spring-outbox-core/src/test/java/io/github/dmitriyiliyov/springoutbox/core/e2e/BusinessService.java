package io.github.dmitriyiliyov.springoutbox.core.e2e;

import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class BusinessService {

    public static final String EVENT_TYPE = "business-event";
    private final OutboxPublisher publisher;
    private final IdPreparer idPreparer;
    private final JdbcTemplate jdbcTemplate;

    @FunctionalInterface
    public interface IdPreparer {
        Object prepare(UUID id);
    }

    public BusinessService(OutboxPublisher publisher, IdPreparer idPreparer, JdbcTemplate jdbcTemplate) {
        this.publisher = publisher;
        this.idPreparer = idPreparer;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void successSaveEvent(BusinessEvent event) {
        String sql = """
            INSERT INTO business_events (verify_id)
            VALUES (?)
        """;
        jdbcTemplate.update(
                sql,
                idPreparer.prepare(event.verifyId())
        );
        publisher.publish(EVENT_TYPE, event);
    }

    @Transactional
    public void successSaveEvents(List<BusinessEvent> events) {
        String sql = """
            INSERT INTO business_events (verify_id)
            VALUES (?)
        """;
        jdbcTemplate.batchUpdate(
                sql,
                events.stream().map(e -> new Object [] {idPreparer.prepare(e.verifyId())}).toList()
        );
        publisher.publish(EVENT_TYPE, events);
    }

    public void exceptionallyWithoutTransaction(BusinessEvent event) {
        String sql = """
            INSERT INTO business_events (verify_id)
            VALUES (?)
        """;
        jdbcTemplate.update(
                sql,
                idPreparer.prepare(event.verifyId())
        );
        publisher.publish(EVENT_TYPE, event);
    }

    public void exceptionallyWithoutTransaction(List<BusinessEvent> events) {
        String sql = """
            INSERT INTO business_events (verify_id)
            VALUES (?)
        """;
        jdbcTemplate.batchUpdate(
                sql,
                events.stream().map(e -> new Object [] {idPreparer.prepare(e.verifyId())}).toList()
        );
        publisher.publish(EVENT_TYPE, events);
    }

    @Transactional
    public void exceptionallyInBusinessTransaction(BusinessEvent event) {
        publisher.publish(EVENT_TYPE, event);
        throw new RuntimeException("Business transaction exception");
    }

    @Transactional
    public void exceptionallyInBusinessTransaction(List<BusinessEvent> events) {
        publisher.publish(EVENT_TYPE, events);
        throw new RuntimeException("Business transaction exception");
    }
}
