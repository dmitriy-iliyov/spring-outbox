package io.github.dmitriyiliyov.springoutbox.core.dlq;

import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class LogOutboxDlqHandler implements OutboxDlqHandler {

    private static final Logger log = LoggerFactory.getLogger(LogOutboxDlqHandler.class);

    @Override
    public void handle(List<OutboxEvent> events) {
        if (events == null || events.isEmpty()) {
            log.warn("DLQ handler invoked with empty event list.");
            return;
        }
        String ids = events.stream()
                .map(e -> String.valueOf(e.getId()))
                .collect(Collectors.joining(", "));
        log.error("DLQ handler received {} failed events: [{}]", events.size(), ids);
    }
}
