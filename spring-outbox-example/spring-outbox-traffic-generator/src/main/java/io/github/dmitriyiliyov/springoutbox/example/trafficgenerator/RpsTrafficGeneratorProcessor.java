package io.github.dmitriyiliyov.springoutbox.example.trafficgenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ConditionalOnProperty(value = "traffic.generator.rps-balancer.enabled", havingValue = "true")
@Service
@RequiredArgsConstructor
@Slf4j
public class RpsTrafficGeneratorProcessor implements TrafficGeneratorProcessor {

    @Value("${traffic.generator.thread-count:10}")
    private int threadCount;

    @Value("${traffic.generator.rps}")
    public int rps;

    private final TrafficGenerator generator;
    private final Random random = new Random();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(threadCount);

    @EventListener(ApplicationReadyEvent.class)
    @Override
    public void generateTraffic() {
        log.info("{} involved", this.getClass().getName());
        long periodNs = 1_000_000_000L / rps;
        executorService.scheduleAtFixedRate(
                () -> generator.generate(randOperation()),
                0,
                periodNs,
                TimeUnit.NANOSECONDS
        );
    }

    private Operation randOperation() {
        Operation[] operations = Operation.values();
        Operation op = operations[random.nextInt(operations.length)];
        log.info("Current operation type: %s".formatted(op));
        return op;
    }
}
