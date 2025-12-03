package io.github.dmitriyiliyov.springoutbox.example.trafficgenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.example.shared.OrderCreateDto;
import io.github.dmitriyiliyov.springoutbox.example.shared.OrderDto;
import io.github.dmitriyiliyov.springoutbox.example.shared.OrderUpdateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimpleTrafficGenerator implements TrafficGenerator {

    @Value("${traffic.generator.target.batch-url}")
    public String batchUrl;

    @Value("${traffic.generator.random-origin}")
    public int randomOrigin;

    @Value("${traffic.generator.random-bound}")
    public int randomBound;

    @Value("${traffic.generator.random-seed}")
    public long randomSeed;

    private final ObjectMapper mapper;
    private final RestClient restClient;
    private final List<Long> managedIds;
    private final Random random = new Random(randomSeed);

    @Override
    public void generate(Operation operation) {
        try {
            if (operation.equals(Operation.CREATE)) {
                String payload = mapper.writeValueAsString(
                        generateCreate(random.nextInt(randomOrigin, randomBound))
                );
                OrderDto [] response = restClient
                        .post()
                        .uri(batchUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(OrderDto[].class);
                if (response != null && response.length > 0) {
                    managedIds.addAll(Arrays.stream(response).map(OrderDto::id).toList());
                } else {
                    log.error("Response is null or empty");
                }
            } else if (operation.equals(Operation.UPDATE)) {
                List<Long> idsToUpdate = generateIds();
                if (idsToUpdate.isEmpty()) {
                    throw new IndexOutOfBoundsException();
                }
                String payload = mapper.writeValueAsString(generateUpdate(idsToUpdate));
                restClient
                        .patch()
                        .uri(batchUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
            } else if (operation.equals(Operation.DELETE)) {
                List<Long> idsToDelete = generateIds();
                if (idsToDelete.isEmpty()) {
                    throw new IndexOutOfBoundsException();
                }
                String payload = mapper.writeValueAsString(idsToDelete);
                restClient
                        .method(HttpMethod.DELETE)
                        .uri(batchUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
                managedIds.removeAll(idsToDelete);
            } else {
                throw new IllegalArgumentException("Unexpected operation type");
            }
        } catch (JsonProcessingException e) {
            log.error("Error when parse payload", e);
        } catch (ResponseStatusException e) {
            log.error("Server returned an HTTP error status", e);
        } catch (RestClientResponseException e) {
            log.error("Error sending request", e);
        } catch (IndexOutOfBoundsException e) {
            log.error("No ids to generate delete or update request", e);
        } catch (Exception e) {
            log.error("Unexpected exception", e);
        }
    }

    private List<Long> generateIds() {
        int size = Math.min(managedIds.size() / 2, randomBound);
        List<Long> ids = new ArrayList<>(managedIds);
        Collections.shuffle(ids);
        return ids.subList(0, size);
    }

    private Long generateUserId() {
        return random.nextLong(1);
    }

    private String generateItemsIds() {
        int count = random.nextInt(1, randomBound);
        String ids = random.ints(count, randomOrigin, randomBound)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining(", "));
        return "[%s]".formatted(ids);
    }

    private List<OrderCreateDto> generateCreate(int count) {
        List<OrderCreateDto> toCreate = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            toCreate.add(new OrderCreateDto(generateUserId(), generateItemsIds()));
        }
        return toCreate;
    }

    private List<OrderUpdateDto> generateUpdate(List<Long> idsToUpdate) {
        List<OrderUpdateDto> toUpdate = new ArrayList<>();
        for (Long id : idsToUpdate) {
            toUpdate.add(new OrderUpdateDto(id, generateItemsIds()));
        }
        return toUpdate;
    }
}
