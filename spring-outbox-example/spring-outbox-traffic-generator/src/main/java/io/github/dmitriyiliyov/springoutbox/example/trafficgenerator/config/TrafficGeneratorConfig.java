package io.github.dmitriyiliyov.springoutbox.example.trafficgenerator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.example.trafficgenerator.DeterminateTrafficGenerator;
import io.github.dmitriyiliyov.springoutbox.example.trafficgenerator.TrafficGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TrafficGeneratorConfig {

    @Value("${traffic.generator.thread-count:10}")
    private int threadCount;

    @Value("${traffic.generator.rps}")
    public int rps;

    @ConditionalOnProperty(value = "traffic.generator.type", havingValue = "determinate")
    @Bean
    public TrafficGenerator determinateTrafficGenerator(ObjectMapper mapper, RestClient restClient) {
        return new DeterminateTrafficGenerator(rps / threadCount, mapper, restClient);
    }
}
