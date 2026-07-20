package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

public class CompositePostApplicationReadyOutboxInitializerIntegrationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withBean(DistributedLockRepository.class, () -> mock(DistributedLockRepository.class))
            .withPropertyValues("oncebox.tables.auto-create=false", "oncebox.publisher.enabled=false");

    @Test
    @DisplayName("IT should call init on all injected PostApplicationReadyOutboxInitializer beans directly")
    void shouldCallInitOnAllInitializersWhenCalledDirectly() {
        contextRunner
                .withBean("mockInitializer1", PostApplicationReadyOutboxInitializer.class, () -> mock(PostApplicationReadyOutboxInitializer.class))
                .withBean("mockInitializer2", PostApplicationReadyOutboxInitializer.class, () -> mock(PostApplicationReadyOutboxInitializer.class))
                .run(ctx -> {
                    PostApplicationReadyOutboxInitializer mock1 = ctx.getBean("mockInitializer1", PostApplicationReadyOutboxInitializer.class);
                    PostApplicationReadyOutboxInitializer mock2 = ctx.getBean("mockInitializer2", PostApplicationReadyOutboxInitializer.class);

                    CompositePostApplicationReadyOutboxInitializer composite =
                            ctx.getBean(CompositePostApplicationReadyOutboxInitializer.class);

                    composite.init();

                    verify(mock1, times(1)).init();
                    verify(mock2, times(1)).init();
                });
    }

    @Test
    @DisplayName("IT should invoke init on nested initializers when ApplicationReadyEvent is fired")
    void shouldInvokeInitWhenApplicationReadyEventFired() {
        contextRunner
                .withBean("mockInitializer", PostApplicationReadyOutboxInitializer.class, () -> mock(PostApplicationReadyOutboxInitializer.class))
                .run(ctx -> {
                    PostApplicationReadyOutboxInitializer mockInitializer = ctx.getBean("mockInitializer", PostApplicationReadyOutboxInitializer.class);

                    ctx.publishEvent(new ApplicationReadyEvent(
                            mock(SpringApplication.class),
                            new String[]{},
                            ctx.getSourceApplicationContext(),
                            Duration.ZERO
                    ));

                    verify(mockInitializer, times(1)).init();
                });
    }

    @Test
    @DisplayName("IT should work gracefully when there are no other initializer beans")
    void shouldWorkWithNoOtherInitializerBeans() {
        contextRunner.run(ctx ->
                assertThatCode(() ->
                        ctx.getBean(CompositePostApplicationReadyOutboxInitializer.class).init()
                ).doesNotThrowAnyException()
        );
    }

    @Test
    @DisplayName("IT should call init twice on nested beans when composite init is called twice")
    void shouldCallInitTwiceWhenInitCalledTwice() {
        contextRunner
                .withBean("mockInitializer", PostApplicationReadyOutboxInitializer.class, () -> mock(PostApplicationReadyOutboxInitializer.class))
                .run(ctx -> {
                    PostApplicationReadyOutboxInitializer mockInitializer = ctx.getBean("mockInitializer", PostApplicationReadyOutboxInitializer.class);
                    CompositePostApplicationReadyOutboxInitializer composite =
                            ctx.getBean(CompositePostApplicationReadyOutboxInitializer.class);

                    composite.init();
                    composite.init();

                    verify(mockInitializer, times(2)).init();
                });
    }
}