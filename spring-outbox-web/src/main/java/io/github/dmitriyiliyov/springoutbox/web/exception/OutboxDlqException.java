package io.github.dmitriyiliyov.springoutbox.web.exception;

public abstract class OutboxDlqException extends RuntimeException {
    public abstract String getDetail();
}
