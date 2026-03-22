package io.github.dmitriyiliyov.springoutbox.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqManager;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.exception.OutboxDlqEventInProcessException;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.exception.OutboxDlqEventNotFoundException;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.projection.BatchRequestProjection;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.projection.BatchUpdateRequestProjection;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(controllers = OutboxDlqController.class)
class OutboxDlqControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OutboxDlqManager manager;

    @Test
    @DisplayName("IT GET /{id} should return 200 with event")
    void get_existingId_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        OutboxDlqEvent event = buildEvent(id);
        when(manager.loadById(id)).thenReturn(event);

        mockMvc.perform(get("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("IT GET /{id} when not found should return 404")
    void get_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.loadById(id)).thenThrow(new OutboxDlqEventNotFoundException("Event not found"));

        mockMvc.perform(get("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Event not found"))
                .andExpect(jsonPath("$.type").value("/errors/outbox/not-found"));
    }

    @Test
    @DisplayName("IT GET /{id} when unexpected error should return 500")
    void get_unexpectedError_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.loadById(id)).thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(get("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.type").value("/errors/outbox/unexpected"));
    }

    @Test
    @DisplayName("IT GET /{id} when database error should return 500")
    void get_databaseError_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.loadById(id)).thenThrow(new DataAccessException("DB error") {});

        mockMvc.perform(get("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/errors/outbox/database"))
                .andExpect(jsonPath("$.title").value("Database Access Error"));
    }

    @Test
    @DisplayName("IT GET / should return 200 with batch")
    void getBatch_validRequest_returns200() throws Exception {
        when(manager.loadBatch(any(BatchRequestProjection.class))).thenReturn(List.of(buildEvent(UUID.randomUUID())));

        mockMvc.perform(get("/api/outbox-dlq/events")
                        .param("status", DlqStatus.RESOLVED.name())
                        .param("batchNumber", "1")
                        .param("batchSize", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT GET / when database error should return 500")
    void getBatch_databaseError_returns500() throws Exception {
        when(manager.loadBatch(any(BatchRequestProjection.class)))
                .thenThrow(new DataAccessException("DB error") {});

        mockMvc.perform(get("/api/outbox-dlq/events")
                        .param("status", DlqStatus.RESOLVED.name())
                        .param("batchNumber", "1")
                        .param("batchSize", "10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/errors/outbox/database"));
    }

    @Test
    @DisplayName("IT PATCH /{id} should return 204")
    void updateStatus_validRequest_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        DlqStatusDto dto = new DlqStatusDto(DlqStatus.RESOLVED);
        doNothing().when(manager).updateStatus(eq(id), any(DlqStatus.class));

        mockMvc.perform(patch("/api/outbox-dlq/events/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("IT PATCH /{id} with null status should return 400")
    void updateStatus_nullStatus_returns400() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/api/outbox-dlq/events/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/errors/outbox/validation"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("IT PATCH /{id} when not found should return 404")
    void updateStatus_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        DlqStatusDto dto = new DlqStatusDto(DlqStatus.RESOLVED);
        doThrow(new OutboxDlqEventNotFoundException("Not found")).when(manager).updateStatus(eq(id), any(DlqStatus.class));

        mockMvc.perform(patch("/api/outbox-dlq/events/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("/errors/outbox/not-found"));
    }

    @Test
    @DisplayName("IT PATCH /{id} when bad request should return 400")
    void updateStatus_badRequest_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        DlqStatusDto dto = new DlqStatusDto(DlqStatus.RESOLVED);
        doThrow(new OutboxDlqEventInProcessException(id))
                .when(manager).updateStatus(eq(id), any(DlqStatus.class));

        mockMvc.perform(patch("/api/outbox-dlq/events/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/errors/outbox/bad-request"))
                .andExpect(jsonPath("$.detail").value("Outbox DLQ event with id=%s is IN_PROCESS, interaction impossible".formatted(id)));
    }

    @Test
    @DisplayName("IT PATCH /{id} when database error should return 500")
    void updateStatus_databaseError_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        DlqStatusDto dto = new DlqStatusDto(DlqStatus.RESOLVED);
        doThrow(new DataAccessException("DB error") {}).when(manager).updateStatus(eq(id), any(DlqStatus.class));

        mockMvc.perform(patch("/api/outbox-dlq/events/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/errors/outbox/database"));
    }

    @Test
    @DisplayName("IT PATCH / should return 204")
    void updateBatchStatus_validRequest_returns204() throws Exception {
        BatchUpdateRequest request = new BatchUpdateRequest(
                Set.of(UUID.randomUUID(), UUID.randomUUID()),
                DlqStatus.RESOLVED
        );
        doNothing().when(manager).updateBatchStatus(any(BatchUpdateRequestProjection.class));

        mockMvc.perform(patch("/api/outbox-dlq/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("IT PATCH / with empty ids should return 400")
    void updateBatchStatus_emptyIds_returns400() throws Exception {
        BatchUpdateRequest request = new BatchUpdateRequest(Set.of(), DlqStatus.RESOLVED);

        mockMvc.perform(patch("/api/outbox-dlq/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/errors/outbox/validation"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("IT PATCH / with null status should return 400")
    void updateBatchStatus_nullStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/outbox-dlq/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\": [\"" + UUID.randomUUID() + "\"], \"status\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("IT PATCH / when not found should return 404")
    void updateBatchStatus_notFound_returns404() throws Exception {
        BatchUpdateRequest request = new BatchUpdateRequest(
                Set.of(UUID.randomUUID()), DlqStatus.RESOLVED
        );
        doThrow(new OutboxDlqEventNotFoundException("Not found"))
                .when(manager).updateBatchStatus(any(BatchUpdateRequestProjection.class));

        mockMvc.perform(patch("/api/outbox-dlq/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("IT PATCH / when database error should return 500")
    void updateBatchStatus_databaseError_returns500() throws Exception {
        BatchUpdateRequest request = new BatchUpdateRequest(
                Set.of(UUID.randomUUID()), DlqStatus.RESOLVED
        );
        doThrow(new DataAccessException("DB error") {})
                .when(manager).updateBatchStatus(any(BatchUpdateRequestProjection.class));

        mockMvc.perform(patch("/api/outbox-dlq/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/errors/outbox/database"));
    }

    @Test
    @DisplayName("IT DELETE /{id} should return 204")
    void delete_existingId_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.deleteById(id)).thenReturn(1);

        mockMvc.perform(delete("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("IT DELETE /{id} when not found should return 404")
    void delete_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.deleteById(id)).thenThrow(new OutboxDlqEventNotFoundException("Not found"));

        mockMvc.perform(delete("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("/errors/outbox/not-found"));
    }

    @Test
    @DisplayName("IT DELETE /{id} when unexpected error should return 500")
    void delete_unexpectedError_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.deleteById(id)).thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(delete("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/errors/outbox/unexpected"));
    }

    @Test
    @DisplayName("IT DELETE /{id} when database error should return 500")
    void delete_databaseError_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.deleteById(id)).thenThrow(new DataAccessException("DB error") {});

        mockMvc.perform(delete("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/errors/outbox/database"));
    }

    @Test
    @DisplayName("IT DELETE / should return 204")
    void deleteBatch_validRequest_returns204() throws Exception {
        DeleteBatchRequest request = new DeleteBatchRequest(
                Set.of(UUID.randomUUID(), UUID.randomUUID())
        );
        when(manager.deleteBatchWithCheck(any())).thenReturn(2);

        mockMvc.perform(delete("/api/outbox-dlq/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("IT DELETE / with empty ids should return 400")
    void deleteBatch_emptyIds_returns400() throws Exception {
        DeleteBatchRequest request = new DeleteBatchRequest(Set.of());

        mockMvc.perform(delete("/api/outbox-dlq/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/errors/outbox/validation"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("IT DELETE / with null ids should return 400")
    void deleteBatch_nullIds_returns400() throws Exception {
        mockMvc.perform(delete("/api/outbox-dlq/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("IT DELETE / when not found should return 404")
    void deleteBatch_notFound_returns404() throws Exception {
        DeleteBatchRequest request = new DeleteBatchRequest(Set.of(UUID.randomUUID()));
        when(manager.deleteBatchWithCheck(any())).thenThrow(new OutboxDlqEventNotFoundException("Not found"));

        mockMvc.perform(delete("/api/outbox-dlq/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("IT DELETE / when database error should return 500")
    void deleteBatch_databaseError_returns500() throws Exception {
        DeleteBatchRequest request = new DeleteBatchRequest(Set.of(UUID.randomUUID()));
        when(manager.deleteBatchWithCheck(any())).thenThrow(new DataAccessException("DB error") {});

        mockMvc.perform(delete("/api/outbox-dlq/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/errors/outbox/database"));
    }

    @Test
    @DisplayName("IT error response contains timestamp and path properties")
    void errorResponse_containsTimestampAndPath() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.loadById(id)).thenThrow(new OutboxDlqEventNotFoundException("Not found"));

        mockMvc.perform(get("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/outbox-dlq/events/" + id))
                .andExpect(jsonPath("$.instance").value("/api/outbox-dlq/events/" + id));
    }

    @Test
    @DisplayName("IT validation error response contains errors array with field and message")
    void validationError_containsFieldErrors() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/api/outbox-dlq/events/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").exists())
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    private OutboxDlqEvent buildEvent(UUID id) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return new OutboxDlqEvent(
                id,
                EventStatus.FAILED,
                "ORDER_CREATED",
                "io.example.OrderCreated",
                "{\"orderId\":\"123\"}",
                0,
                now.plusSeconds(60),
                now,
                now,
                DlqStatus.MOVED,
                now
        );
    }
}