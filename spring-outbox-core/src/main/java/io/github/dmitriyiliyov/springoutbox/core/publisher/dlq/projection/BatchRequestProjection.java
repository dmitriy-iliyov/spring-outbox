package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.projection;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;

public interface BatchRequestProjection {
    DlqStatus status();

    int batchNumber();

    int batchSize();
}
