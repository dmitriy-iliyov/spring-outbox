package io.github.dmitriyiliyov.oncebox.consumer.cache;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RedisIntegrationTestsConfig.class)
public class TestApplication { }