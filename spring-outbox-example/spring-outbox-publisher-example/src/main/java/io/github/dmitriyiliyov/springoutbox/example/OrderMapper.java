package io.github.dmitriyiliyov.springoutbox.example;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    Order toEntity(OrderCreateDto dto);

    OrderResponseDto toDto(Order entity);
}
