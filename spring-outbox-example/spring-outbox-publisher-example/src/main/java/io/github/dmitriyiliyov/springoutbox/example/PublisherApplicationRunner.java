package io.github.dmitriyiliyov.springoutbox.example;

import io.github.dmitriyiliyov.springoutbox.config.EnableOutbox;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
@EnableKafka
@EnableOutbox
public class PublisherApplicationRunner {

    public static void main(String ... args) {
        SpringApplication.run(PublisherApplicationRunner.class, args);
    }
}
