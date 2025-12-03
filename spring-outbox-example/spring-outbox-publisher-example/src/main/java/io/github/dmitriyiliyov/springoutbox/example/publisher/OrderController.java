package io.github.dmitriyiliyov.springoutbox.example.publisher;

import io.github.dmitriyiliyov.springoutbox.example.shared.OrderCreateDto;
import io.github.dmitriyiliyov.springoutbox.example.shared.OrderUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody OrderCreateDto dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.save(dto));
    }

    @PostMapping("/batch")
    public ResponseEntity<?> createBatch(@RequestBody List<OrderCreateDto> dtoList) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.saveBatch(dtoList));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(service.findAll());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody OrderUpdateDto dto) {
        dto.setId(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(service.update(dto));
    }

    @PatchMapping("/batch")
    public ResponseEntity<?> updateBatch(@RequestBody List<OrderUpdateDto> dtoList) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(service.updateBatch(dtoList));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @DeleteMapping("/batch")
    public ResponseEntity<?> deleteBatch(@RequestBody List<Long> ids) {
        service.deleteBatch(ids);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
