package io.github.dmitriyiliyov.springoutbox.example.shared;

import java.math.BigDecimal;

public record OrderResponseDto(
        Long id,
        Long userId,
        String itemIds,
        BigDecimal amount
) { }
