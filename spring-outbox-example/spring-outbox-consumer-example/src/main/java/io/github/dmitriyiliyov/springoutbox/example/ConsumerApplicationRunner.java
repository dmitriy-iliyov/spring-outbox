package io.github.dmitriyiliyov.springoutbox.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
@EnableKafka
public class ConsumerApplicationRunner {

    public static void main(String [] args) {
        SpringApplication.run(ConsumerApplicationRunner.class, args);
    }
}
