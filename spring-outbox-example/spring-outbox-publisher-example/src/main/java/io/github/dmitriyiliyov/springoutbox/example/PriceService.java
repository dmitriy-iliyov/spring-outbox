package io.github.dmitriyiliyov.springoutbox.example;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PriceService {

    public BigDecimal countAmount(String itemIds) {
        return new BigDecimal(itemIds.length() * 37);
    }
}
