package io.github.dmitriyiliyov.springoutbox.tests.e2e;

import io.github.dmitriyiliyov.springoutbox.aop.OutboxPublish;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class BusinessService {

    public static final String EVENT_TYPE = "business-event";
    private final IdPreparer idPreparer;
    private final JdbcTemplate jdbcTemplate;

    @FunctionalInterface
    public interface IdPreparer {
        Object prepare(UUID id);
    }

    public BusinessService(IdPreparer idPreparer, JdbcTemplate jdbcTemplate) {
        this.idPreparer = idPreparer;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE, payload = "#event")
    public void successSaveEvent(BusinessEvent event) {
        String sql = """
            INSERT INTO business_events (verify_id)
            VALUES (?)
        """;
        jdbcTemplate.update(
                sql,
                idPreparer.prepare(event.verifyId())
        );
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE, payload = "#events")
    public void successSaveEvents(List<BusinessEvent> events) {
        String sql = """
            INSERT INTO business_events (verify_id)
            VALUES (?)
        """;
        jdbcTemplate.batchUpdate(
                sql,
                events.stream().map(e -> new Object [] {idPreparer.prepare(e.verifyId())}).toList()
        );
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE)
    public BusinessEvent successSaveReturnedEvent(BusinessEvent event) {
        String sql = """
            INSERT INTO business_events (verify_id)
            VALUES (?)
        """;
        jdbcTemplate.update(
                sql,
                idPreparer.prepare(event.verifyId())
        );
        return event;
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE)
    public List<BusinessEvent> successSaveReturnedEvents(List<BusinessEvent> events) {
        String sql = """
            INSERT INTO business_events (verify_id)
            VALUES (?)
        """;
        jdbcTemplate.batchUpdate(
                sql,
                events.stream().map(e -> new Object [] {idPreparer.prepare(e.verifyId())}).toList()
        );
        return events;
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE, payload = "#event")
    public void exceptionallyInBusinessTransaction(BusinessEvent event) {
        throw new RuntimeException("Business transaction exception");
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE, payload = "#events")
    public void exceptionallyInBusinessTransaction(List<BusinessEvent> events) {
        throw new RuntimeException("Business transaction exception");
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE, payload = "#event")
    public void exceptionallyInBusinessTransactionWithReturnedEvent(BusinessEvent event) {
        throw new RuntimeException("Business transaction exception");
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE, payload = "#events")
    public void exceptionallyInBusinessTransactionWithReturnedEvents(List<BusinessEvent> events) {
        throw new RuntimeException("Business transaction exception");
    }
}
