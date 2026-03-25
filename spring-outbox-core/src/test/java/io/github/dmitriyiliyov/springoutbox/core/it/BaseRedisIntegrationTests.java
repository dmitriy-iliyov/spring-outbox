package io.github.dmitriyiliyov.springoutbox.core.it;

import io.github.dmitriyiliyov.springoutbox.core.it.conteiners.RedisTestContainerSingleton;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = RedisTestApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("redis-it")
public abstract class BaseRedisIntegrationTests {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", RedisTestContainerSingleton.INSTANCE::getHost);
        registry.add("spring.data.redis.port", RedisTestContainerSingleton.INSTANCE::getFirstMappedPort);
    }
}
