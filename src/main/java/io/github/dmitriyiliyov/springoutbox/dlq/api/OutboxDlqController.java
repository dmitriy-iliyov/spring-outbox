package io.github.dmitriyiliyov.springoutbox.dlq.api;

import io.github.dmitriyiliyov.springoutbox.dlq.OutboxDlqManager;
import io.github.dmitriyiliyov.springoutbox.dlq.dto.BatchRequest;
import io.github.dmitriyiliyov.springoutbox.dlq.dto.BatchUpdateRequest;
import io.github.dmitriyiliyov.springoutbox.dlq.dto.DeleteBatchRequest;
import io.github.dmitriyiliyov.springoutbox.dlq.dto.DlqStatusDto;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/outbox-dlq/events")
@ConditionalOnProperty(prefix = "outbox.dlq", name = "enable", havingValue = "true")
public class OutboxDlqController {

    private final OutboxDlqManager manager;

    public OutboxDlqController(OutboxDlqManager manager) {
        this.manager = manager;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") UUID id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(manager.loadById(id));
    }

    @GetMapping
    public ResponseEntity<?> getBatch(@ModelAttribute @Valid BatchRequest request) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(manager.loadBatch(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateStatus(@PathVariable("id") UUID id, @RequestBody @Valid DlqStatusDto dto) {
        manager.updateStatus(id, dto.status());
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PatchMapping
    public ResponseEntity<?> updateBatchStatus(@RequestBody @Valid BatchUpdateRequest request) {
        manager.updateBatchStatus(request);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") UUID id) {
        manager.deleteById(id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @DeleteMapping
    public ResponseEntity<?> deleteBatch(@RequestBody @Valid DeleteBatchRequest request) {
        manager.deleteBatch(request.ids());
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
