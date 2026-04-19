package io.github.dmitriyiliyov.springoutbox.example.producer;

import io.github.dmitriyiliyov.springoutbox.example.shared.OrderCreateDto;
import io.github.dmitriyiliyov.springoutbox.example.shared.OrderDto;
import io.github.dmitriyiliyov.springoutbox.example.shared.OrderUpdateDto;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto create(@RequestBody OrderCreateDto dto) {
        return service.save(dto);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<OrderDto> createBatch(@RequestBody List<OrderCreateDto> dtos) {
        return service.saveBatch(dtos);
    }

    @GetMapping
    public List<OrderDto> getAll(@NonNull @PageableDefault(size = 50) Pageable pageable) {
        return service.findAll(pageable);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public OrderDto update(@PathVariable("id") Long id, @RequestBody OrderUpdateDto dto) {
        dto.setId(id);
        return service.update(dto);
    }

    @PatchMapping("/batch")
    @ResponseStatus(HttpStatus.OK)
    public List<OrderDto> updateBatch(@RequestBody List<OrderUpdateDto> dtos) {
        return service.updateBatch(dtos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        service.delete(id);
    }

    @DeleteMapping("/batch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBatch(@RequestBody List<Long> ids) {
        service.deleteBatch(ids);
    }
}
