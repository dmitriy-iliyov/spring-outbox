package io.github.dmitriyiliyov.springoutbox.dlq.api;

import io.github.dmitriyiliyov.springoutbox.dlq.DlqStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public final class DlqStatusQueryConverter implements Converter<String, DlqStatus> {
    @Override
    public DlqStatus convert(String source) {
        return DlqStatus.fromString(source);
    }
}
