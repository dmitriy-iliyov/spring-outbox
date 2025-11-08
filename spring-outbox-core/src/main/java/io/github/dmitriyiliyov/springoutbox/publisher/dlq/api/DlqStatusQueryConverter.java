package io.github.dmitriyiliyov.springoutbox.publisher.dlq.api;

import io.github.dmitriyiliyov.springoutbox.publisher.dlq.DlqStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public final class DlqStatusQueryConverter implements Converter<String, DlqStatus> {
    @Override
    public DlqStatus convert(String source) {
        return DlqStatus.fromString(source);
    }
}
