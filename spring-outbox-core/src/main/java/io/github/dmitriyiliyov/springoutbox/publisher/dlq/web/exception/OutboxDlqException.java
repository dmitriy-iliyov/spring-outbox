package io.github.dmitriyiliyov.springoutbox.publisher.dlq.web.exception;

public abstract class OutboxDlqException extends RuntimeException {
    public abstract String getDetail();
}
