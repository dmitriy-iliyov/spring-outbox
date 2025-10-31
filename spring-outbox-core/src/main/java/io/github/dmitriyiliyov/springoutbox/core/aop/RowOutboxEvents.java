package io.github.dmitriyiliyov.springoutbox.core.aop;

import java.util.List;

public record RowOutboxEvents(
    String eventType,
    List<?> events
) { }
