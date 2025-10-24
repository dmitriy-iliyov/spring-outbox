package io.github.dmitriyiliyov.springoutbox.dlq.api;

import io.github.dmitriyiliyov.springoutbox.dlq.DlqStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "outbox.dlq", name = "enable", havingValue = "true")
public final class DlqStatusQueryConverter implements Converter<String, DlqStatus> {
    @Override
    public DlqStatus convert(String source) {
        return DlqStatus.fromString(source);
    }
}
