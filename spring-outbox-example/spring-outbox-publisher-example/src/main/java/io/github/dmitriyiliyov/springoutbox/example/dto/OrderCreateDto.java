package io.github.dmitriyiliyov.springoutbox.example.dto;

public record OrderCreateDto(
        Long userId,
        String itemIds
) { }
