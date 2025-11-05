package io.github.dmitriyiliyov.springoutbox.example;

import io.github.dmitriyiliyov.springoutbox.example.shared.OrderDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;


/**
 * It is obvious that in a real-world microservices architecture, different services would typically
 * subscribe to different topics based on their domain responsibilities.
 * This example consumer listens to all order-related topics for demonstration purposes only,
 * simulating an analytics service that needs to track all order lifecycle events.
 */
@Slf4j
public class OrderAnalyticKafkaListener {


    @KafkaListener(topics = {"orders.created", "orders.updated", "orders.delete"}, groupId = "analytic")
    public void listenCreateOrder(OrderDto dto) {
        log.info("Analytics receive {}", dto);
    }
}

