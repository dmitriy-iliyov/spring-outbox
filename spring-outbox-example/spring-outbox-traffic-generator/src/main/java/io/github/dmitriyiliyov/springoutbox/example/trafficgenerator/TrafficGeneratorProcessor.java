package io.github.dmitriyiliyov.springoutbox.example.trafficgenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrafficGeneratorProcessor {

    private final TrafficGenerator generator;
    private final Random random = new Random();

    @EventListener(ApplicationReadyEvent.class)
    public void generateTraffic() {
        new Thread(() -> {
            Operation[] operations = Operation.values();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Operation op = operations[random.nextInt(operations.length)];
                    log.info("Current operation type: %s".formatted(op));
                    generator.generate(op);
                    Thread.sleep(random.nextInt(100, 500));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
}
