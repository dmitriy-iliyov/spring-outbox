package io.github.dmitriyiliyov.springoutbox.publisher.dlq.api.exception;

public abstract class OutboxDlqException extends RuntimeException {
    public abstract String getDetail();
}
