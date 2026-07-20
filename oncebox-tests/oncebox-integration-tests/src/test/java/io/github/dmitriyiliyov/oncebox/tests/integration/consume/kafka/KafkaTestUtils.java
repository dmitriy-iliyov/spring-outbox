package io.github.dmitriyiliyov.oncebox.tests.integration.consume.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DeleteRecordsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.common.TopicPartition;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class KafkaTestUtils {

    private final AdminClient adminClient;

    public KafkaTestUtils(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    public void resetKafkaTopics(List<String> topics) {
        try {
            for (String topic : topics) {
                Map<TopicPartition, Long> endOffsets = adminClient
                        .listTopics().names().get().contains(topic) ?
                        adminClient
                                .describeTopics(Collections.singletonList(topic))
                                .all().get().get(topic)
                                .partitions().stream()
                                .collect(
                                        java.util.stream.Collectors.toMap(
                                                p -> new TopicPartition(topic, p.partition()),
                                                p -> {
                                                    try {
                                                        return adminClient.listOffsets(
                                                                Map.of(new TopicPartition(topic, p.partition()), OffsetSpec.latest())
                                                        ).all().get().get(new TopicPartition(topic, p.partition())).offset();
                                                    } catch (InterruptedException e) {
                                                        throw new RuntimeException(e);
                                                    } catch (ExecutionException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                }
                                        )
                                ) : Collections.emptyMap();

                if (!endOffsets.isEmpty()) {
                    DeleteRecordsResult result = adminClient.deleteRecords(
                            endOffsets.entrySet().stream()
                                    .collect(java.util.stream.Collectors.toMap(
                                            Map.Entry::getKey,
                                            e -> RecordsToDelete.beforeOffset(e.getValue())
                                    ))
                    );
                    result.all().get();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to reset Kafka topics", e);
        }
    }
}