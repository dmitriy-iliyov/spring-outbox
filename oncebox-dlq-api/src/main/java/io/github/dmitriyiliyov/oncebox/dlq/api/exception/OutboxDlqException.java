package io.github.dmitriyiliyov.oncebox.dlq.api.exception;

public abstract class OutboxDlqException extends RuntimeException {
    public abstract String getDetail();
}
