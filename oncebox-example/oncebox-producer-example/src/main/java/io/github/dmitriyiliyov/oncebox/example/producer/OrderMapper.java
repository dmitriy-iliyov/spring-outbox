package io.github.dmitriyiliyov.oncebox.example.producer;

import io.github.dmitriyiliyov.oncebox.example.shared.OrderCreateDto;
import io.github.dmitriyiliyov.oncebox.example.shared.OrderDto;
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
