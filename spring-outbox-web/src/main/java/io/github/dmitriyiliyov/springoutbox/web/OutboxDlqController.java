package io.github.dmitriyiliyov.springoutbox.web;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqManager;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/outbox-dlq/events")
public class OutboxDlqController {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqController.class);
    private final OutboxDlqManager manager;

    public OutboxDlqController(OutboxDlqManager manager) {
        this.manager = manager;
        log.warn("OutboxDlqController on '/api/outbox-dlq/events' path should be secured");
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
        manager.deleteBatchWithCheck(request.ids());
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
