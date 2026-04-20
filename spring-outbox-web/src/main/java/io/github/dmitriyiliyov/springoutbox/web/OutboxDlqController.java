
package io.github.dmitriyiliyov.springoutbox.web;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqEvent;
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

    private final OutboxDlqWebManager manager;

    public OutboxDlqController(OutboxDlqWebManager manager) {
        this.manager = manager;
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
        return manager.findById(id);
    }

    @Operation(summary = "Get events by DLQ status with pagination")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "Event batch successfully retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OutboxDlqEvent.class)))
            )
    })
    @GetMapping("/batch")
    public List<OutboxDlqEvent> getBatch(@ModelAttribute @Valid BatchRequest request) {
        return manager.findBatch(request);
    }

    @Operation(summary = "Count DLQ events by status")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "Event count successfully retrieved",
                    content = @Content(schema = @Schema(implementation = Long.class))
            )
    })
    @GetMapping("/count")
    public Long getCount(@Parameter(description = "DLQ event status to count events")
                         @RequestParam(value = "status", required = false) DlqStatus status) {
        return manager.count(status);
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
        manager.updateStatus(id, dto.status());
    }

    @Operation(summary = "Update DLQ status for multiple events by ids")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Event batch successfully updated")
    })
    @PatchMapping("/batch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateBatchStatus(@RequestBody @Valid BatchUpdateRequest request) {
        manager.updateBatchStatus(request);
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
        manager.deleteById(id);
    }

    @Operation(summary = "Delete multiple events by ids")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Event batch successfully deleted")
    })
    @DeleteMapping("/batch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBatch(@RequestBody @Valid DeleteBatchRequest request) {
        manager.deleteBatch(request.ids());
    }
}
