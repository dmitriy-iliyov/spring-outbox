package io.github.dmitriyiliyov.springoutbox.aop;

import java.util.List;

public record RowOutboxEvents(
    String eventType,
    List<?> events
) { }
