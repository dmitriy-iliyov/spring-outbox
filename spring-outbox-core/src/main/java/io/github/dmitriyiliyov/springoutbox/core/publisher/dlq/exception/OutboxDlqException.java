package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.exception;

public abstract class OutboxDlqException extends RuntimeException {
    public abstract String getDetail();
}
