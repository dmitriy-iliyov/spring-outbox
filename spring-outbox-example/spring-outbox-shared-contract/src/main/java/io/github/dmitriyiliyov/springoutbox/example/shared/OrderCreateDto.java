package io.github.dmitriyiliyov.springoutbox.example.shared;

public record OrderCreateDto(
        Long userId,
        String itemIds
) { }
