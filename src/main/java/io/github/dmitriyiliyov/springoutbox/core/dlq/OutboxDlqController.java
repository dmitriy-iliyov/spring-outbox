package io.github.dmitriyiliyov.springoutbox.core.dlq;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(name = "outbox.dlq.enabled", havingValue = "true")
public class OutboxDlqController {
}
