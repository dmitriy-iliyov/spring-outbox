package io.github.dmitriyiliyov.springoutbox.publisher.dlq.web.exception;

import java.util.UUID;

public class OutboxDlqEventNotFoundException extends NotFoundException {

    private UUID id;
    private String detail = "No OutboxDlqEvent found with id=";

    public OutboxDlqEventNotFoundException(UUID id) {
        this.id = id;
    }

    public OutboxDlqEventNotFoundException(String detail) {
        this.detail = detail;
    }

    @Override
    public String getDetail() {
        if (id == null) {
            return detail;
        }
        return detail + id;
    }
}
