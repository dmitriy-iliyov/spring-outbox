package io.github.dmitriyiliyov.springoutbox.example;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@NoArgsConstructor
public class Order {

    @Id
    private Long id;


}
