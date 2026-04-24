package io.github.dmitriyiliyov.springoutbox.dlq.api.exception;

public class UnknownDlqStatusException extends BadRequestException {

    private final String source;

    public UnknownDlqStatusException(String source) {
        this.source = source;
    }

    @Override
    public String getDetail() {
        return "Unknown value '%s' of enum io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus".formatted(source);
    }
}
