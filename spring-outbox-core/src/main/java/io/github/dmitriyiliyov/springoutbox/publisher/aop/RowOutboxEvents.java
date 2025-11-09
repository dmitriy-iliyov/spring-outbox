package io.github.dmitriyiliyov.springoutbox.publisher.aop;

import java.util.List;

public record RowOutboxEvents(
    String eventType,
    List<?> events
) { }
