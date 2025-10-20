package io.github.dmitriyiliyov.springoutbox.core.dlq;

import java.util.List;

public interface OutboxDlqManager {
    void saveBatch(List<OutboxDlqEvent> events);
}
