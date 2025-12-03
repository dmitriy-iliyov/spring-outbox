package io.github.dmitriyiliyov.springoutbox.example.shared;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateDto {
    private Long id;
    private String itemIds;
}
