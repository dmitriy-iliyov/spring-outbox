package io.github.dmitriyiliyov.springoutbox.example;

import io.github.dmitriyiliyov.springoutbox.example.dto.OrderCreateDto;
import io.github.dmitriyiliyov.springoutbox.example.shared.OrderDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    Order toEntity(OrderCreateDto dto);

    List<Order> toEntityList(List<OrderCreateDto> dtoList);

    OrderDto toDto(Order entity);

    List<OrderDto> toDtoList(List<Order> entity);
}
