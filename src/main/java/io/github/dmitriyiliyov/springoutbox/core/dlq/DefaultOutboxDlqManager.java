package io.github.dmitriyiliyov.springoutbox.core.dlq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class DefaultOutboxDlqManager implements OutboxDlqManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxDlqManager.class);
    private final OutboxDlqRepository repository;

    public DefaultOutboxDlqManager(OutboxDlqRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @Override
    public void saveBatch(List<OutboxDlqEvent> events) {
        repository.saveBatch(events);
    }


}
