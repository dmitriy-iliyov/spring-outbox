package io.github.dmitriyiliyov.springoutbox.dlq.api;

public abstract class NotFoundException extends RuntimeException {

    public abstract String getDetail();
}
