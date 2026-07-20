
package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.OutboxDlqEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/outbox-dlq/events")
public class OutboxDlqController {

    private final OutboxDlqApiService service;

    public OutboxDlqController(OutboxDlqApiService service) {
        this.service = service;
    }

    @Operation(summary = "Get event by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event successfully retrieved",
                    content = @Content(schema = @Schema(implementation = OutboxDlqEvent.class))),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @GetMapping("/{id}")
    public OutboxDlqEvent get(@Parameter(description = "Id of the DLQ event", required = true)
                              @PathVariable("id") UUID id) {
        return service.findById(id);
    }

    @Operation(summary = "DLQ events pagination by status and/or event type (paginating of all events if no parameters are provided")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "Event batch successfully retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OutboxDlqEvent.class)))
            )
    })
    @GetMapping("/batch")
    public List<OutboxDlqEvent> getBatch(@ModelAttribute @Valid BatchRequest request) {
        return service.findBatch(request);
    }

    @Operation(summary = "Count DLQ events by status and/or event type (counts all events if no parameters are provided)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "Event count successfully retrieved",
                    content = @Content(schema = @Schema(implementation = Long.class))
            )
    })
    @GetMapping("/count")
    public Long getCount(@Parameter(description = "DLQ event status to processedCount events")
                         @RequestParam(value = "status", required = false) DlqStatus status,
                         @Parameter(description = "Event type to processedCount events")
                         @RequestParam(value = "eventType", required = false) String eventType) {
        return service.count(status, eventType);
    }

    @Operation(summary = "Update event's DLQ status")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Event successfully updated"),
            @ApiResponse(responseCode = "404", description = "Event not found"),
    })
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(@Parameter(description = "Id of the DLQ event to update", required = true)
                             @PathVariable("id") UUID id,
                             @RequestBody @Valid DlqStatusDto dto) {
        service.updateStatus(id, dto.status());
    }

    @Operation(summary = "Update DLQ status for multiple events by ids or event type")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "Event batch successfully updated",
                    content = @Content(schema = @Schema(implementation = BatchModificationResponse.class))
            )
    })
    @PatchMapping("/batch")
    @ResponseStatus(HttpStatus.OK)
    public BatchModificationResponse updateBatchStatus(@RequestBody @Valid BatchUpdateRequest request) {
        return service.updateBatchStatus(request);
    }

    @Operation(summary = "Delete event by id")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Event successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Event not found"),
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Id of the DLQ event to delete", required = true)
                       @PathVariable("id") UUID id) {
        service.deleteById(id);
    }

    @Operation(summary = "Delete multiple events by ids or event type")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204", description = "Event batch successfully deleted",
                    content = @Content(schema = @Schema(implementation = BatchModificationResponse.class))
            )
    })
    @DeleteMapping("/batch")
    @ResponseStatus(HttpStatus.OK)
    public BatchModificationResponse deleteBatch(@RequestBody @Valid BatchDeleteRequest request) {
        return service.deleteBatch(request);
    }
}