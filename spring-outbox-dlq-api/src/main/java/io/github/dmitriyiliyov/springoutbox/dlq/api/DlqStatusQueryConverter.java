package io.github.dmitriyiliyov.springoutbox.dlq.api;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.dlq.api.exception.UnknownDlqStatusException;
import jakarta.annotation.Nullable;
import org.springframework.core.convert.converter.Converter;

public class DlqStatusQueryConverter implements Converter<String, DlqStatus> {
    @Override
    public DlqStatus convert(@Nullable String source) {
        try {
            return DlqStatus.fromString(source);
        } catch (Exception e) {
             throw new UnknownDlqStatusException(source);
        }
    }
}
