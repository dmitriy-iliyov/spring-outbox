package io.github.dmitriyiliyov.springoutbox.example.dto;

import lombok.Data;

@Data
public class OrderUpdateDto {
    private Long id;
    private String itemIds;
}
