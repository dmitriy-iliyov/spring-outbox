package io.github.dmitriyiliyov.springoutbox.example.trafficgenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.example.shared.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@ConditionalOnProperty(value = "traffic.generator.type", havingValue = "stochastic")
@Service
@Slf4j
@RequiredArgsConstructor
public class StochasticTrafficGenerator implements TrafficGenerator {

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
    private final List<Long> managedIds = Collections.synchronizedList(new ArrayList<>());
    private final Random random = new Random(randomSeed);

    @Override
    public void generate(Operation operation) {
        try {
            if (operation.equals(Operation.CREATE)) {
                String payload = mapper.writeValueAsString(
                        GeneratorUtils.generateCreateDtoList(
                                random.nextInt(randomOrigin, randomBound),
                                () -> random.nextLong(1),
                                () -> GeneratorUtils.generateItemsIds(random, randomOrigin)
                        )
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
                List<Long> idsToUpdate = GeneratorUtils.generateIds(randomOrigin, managedIds);
                if (idsToUpdate.isEmpty()) {
                    return;
                }
                String payload = mapper.writeValueAsString(
                        GeneratorUtils.generateUpdateDtoList(
                                idsToUpdate,
                                () -> GeneratorUtils.generateItemsIds(random, randomOrigin)
                        )
                );
                restClient
                        .patch()
                        .uri(batchUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
            } else if (operation.equals(Operation.DELETE)) {
                List<Long> idsToDelete = GeneratorUtils.generateIds(randomOrigin, managedIds);
                if (idsToDelete.isEmpty()) {
                    return;
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
}
