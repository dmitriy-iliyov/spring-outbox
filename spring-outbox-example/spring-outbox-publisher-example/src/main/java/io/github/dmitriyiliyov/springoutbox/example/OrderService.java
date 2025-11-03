package io.github.dmitriyiliyov.springoutbox.example;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisher;
import io.github.dmitriyiliyov.springoutbox.core.aop.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.example.dto.OrderCreateDto;
import io.github.dmitriyiliyov.springoutbox.example.dto.SpecificOrderDto;
import io.github.dmitriyiliyov.springoutbox.example.dto.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final OutboxPublisher outboxPublisher;

    @Transactional
    @OutboxEvent(eventType = "create-order")
    @Override
    public OrderResponseDto save(OrderCreateDto dto) {
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }

    @Transactional
    @OutboxEvent(eventType = "create-order")
    @Override
    public List<OrderResponseDto> saveBatch(List<OrderCreateDto> dtoList) {
        return mapper.toDtoList(repository.saveAll(mapper.toEntityList(dtoList)));
    }

    @Transactional
    @OutboxEvent(eventType = "create-order", payload = "#dto")
    @Override
    public OrderResponseDto save(OrderCreateDto dto) {
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }

    @Transactional
    @OutboxEvent(eventType = "create-order", payload = "#dtoList")
    @Override
    public List<OrderResponseDto> saveBatch(List<OrderCreateDto> dtoList) {
        return mapper.toDtoList(repository.saveAll(mapper.toEntityList(dtoList)));
    }

    @Transactional
    @Override
    public OrderResponseDto save(OrderCreateDto dto) {
        Order order = repository.save(mapper.toEntity(dto));
        outboxPublisher.publish("create-order", mapper.toSpecificDto(order));
        return mapper.toDto(order);
    }

    @Transactional
    @Override
    public List<OrderResponseDto> saveBatch(List<OrderCreateDto> dtoList) {
        List<Order> orderList = repository.saveAll(mapper.toEntityList(dtoList));
        outboxPublisher.publish("create-order", mapper.toSpecificList(orderList));
        return mapper.toDtoList(orderList);
    }
}
