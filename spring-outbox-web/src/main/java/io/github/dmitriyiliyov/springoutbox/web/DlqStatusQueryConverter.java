package io.github.dmitriyiliyov.springoutbox.web;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class DlqStatusQueryConverter implements Converter<String, DlqStatus> {
    @Override
    public DlqStatus convert(String source) {
        return DlqStatus.fromString(source);
    }
}
