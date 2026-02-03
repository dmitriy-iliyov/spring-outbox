package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.projection;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;

import java.util.Set;
import java.util.UUID;

public interface BatchUpdateRequestProjection {
    Set<UUID> ids();
    DlqStatus status();
}
