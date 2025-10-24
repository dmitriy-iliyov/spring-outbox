package io.github.dmitriyiliyov.springoutbox.dlq.api;

import java.util.UUID;

public class OutboxDlqEventNotFoundException extends NotFoundException {

    private UUID id;
    private String exceptionDetail = "No OutboxDlqEvent found with id=";

    public OutboxDlqEventNotFoundException(UUID id) {
        this.id = id;
    }

    public OutboxDlqEventNotFoundException(String exceptionDetail) {
        this.exceptionDetail = exceptionDetail;
    }

    @Override
    public String getDetail() {
        if (id == null) {
            return exceptionDetail;
        }
        return exceptionDetail + id;
    }
}
