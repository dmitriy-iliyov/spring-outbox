package io.github.dmitriyiliyov.springoutbox.dlq.api.exception;

public class InvalidDlqFilterException extends BadRequestException {

    private final String detail;

    public InvalidDlqFilterException(String detail) {
        this.detail = detail;
    }

    @Override
    public String getDetail() {
        return detail;
    }
}
