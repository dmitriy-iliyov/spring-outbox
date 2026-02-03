package io.github.dmitriyiliyov.springoutbox.kafka;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxHeaders;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaOutboxEventIdResolverUnitTests {

    private final KafkaOutboxEventIdResolver resolver = new KafkaOutboxEventIdResolver();

    @Test
    @DisplayName("UT resolve() should return UUID from headers")
    void resolve_shouldReturnUuidFromHeaders() {
        // given
        UUID expectedId = UUID.randomUUID();
        Header header = new RecordHeader(OutboxHeaders.EVENT_ID.getValue(),
                expectedId.toString().getBytes(StandardCharsets.UTF_8));
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, "key", "value");
        record.headers().add(header);

        // when
        UUID result = resolver.resolve(record);

        // then
        assertThat(result).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("UT resolve() should throw if header missing")
    void resolve_shouldThrowIfHeaderMissing() {
        // given
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, "key", "value");

        // when + then
        assertThatThrownBy(() -> resolver.resolve(record))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Header '%s' not found".formatted(OutboxHeaders.EVENT_ID.getValue()));
    }

    @Test
    @DisplayName("UT getSupports() should return ConsumerRecord.class")
    void getSupports_shouldReturnConsumerRecordClass() {
        // when
        Class<?> result = resolver.getSupports();

        // then
        assertThat(result).isEqualTo(ConsumerRecord.class);
    }
}
