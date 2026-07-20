package io.github.dmitriyiliyov.oncebox.example.shared;

public record OrderCreateDto(
        Long userId,
        String itemIds
) { }
