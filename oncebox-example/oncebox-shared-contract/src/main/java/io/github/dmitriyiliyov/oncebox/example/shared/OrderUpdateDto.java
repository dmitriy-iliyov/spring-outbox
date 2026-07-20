package io.github.dmitriyiliyov.oncebox.example.shared;

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
