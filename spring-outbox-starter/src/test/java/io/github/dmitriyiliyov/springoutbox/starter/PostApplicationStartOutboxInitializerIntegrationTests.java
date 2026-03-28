package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;


public class PostApplicationStartOutboxInitializerIntegrationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withPropertyValues("outbox.tables.auto-create=false");

    @Test
    @DisplayName("IT should call schedule on all OutboxScheduler beans after application ready")
    void shouldCallScheduleOnAllSchedulersAfterApplicationReady() {
        contextRunner
                .withBean("testScheduler1", OutboxScheduler.class, () -> mock(OutboxScheduler.class))
                .withBean("testScheduler2", OutboxScheduler.class, () -> mock(OutboxScheduler.class))
                .run(ctx -> {
                    OutboxScheduler scheduler1 = ctx.getBean("testScheduler1", OutboxScheduler.class);
                    OutboxScheduler scheduler2 = ctx.getBean("testScheduler2", OutboxScheduler.class);

                    ctx.getBean(PostApplicationStartOutboxInitializer.class).init();

                    verify(scheduler1, times(1)).schedule();
                    verify(scheduler2, times(1)).schedule();
                });
    }

    @Test
    @DisplayName("IT should call register on all OutboxMetrics beans after application ready")
    void shouldCallRegisterOnAllMetricsAfterApplicationReady() {
        contextRunner
                .withBean("testMetrics1", OutboxMetrics.class, () -> mock(OutboxMetrics.class))
                .withBean("testMetrics2", OutboxMetrics.class, () -> mock(OutboxMetrics.class))
                .run(ctx -> {
                    OutboxMetrics metrics1 = ctx.getBean("testMetrics1", OutboxMetrics.class);
                    OutboxMetrics metrics2 = ctx.getBean("testMetrics2", OutboxMetrics.class);

                    ctx.getBean(PostApplicationStartOutboxInitializer.class).init();

                    verify(metrics1, times(1)).register();
                    verify(metrics2, times(1)).register();
                });
    }

    @Test
    @DisplayName("IT should invoke init when ApplicationReadyEvent is fired")
    void shouldInvokeInitWhenApplicationReadyEventFired() {
        contextRunner
                .withBean("testScheduler", OutboxScheduler.class, () -> mock(OutboxScheduler.class))
                .run(ctx -> {
                    OutboxScheduler scheduler = ctx.getBean("testScheduler", OutboxScheduler.class);

                    ConfigurableApplicationContext configurableCtx =
                            (ConfigurableApplicationContext) ctx.getSourceApplicationContext();
                    configurableCtx.publishEvent(
                            new ApplicationReadyEvent(
                                    mock(org.springframework.boot.SpringApplication.class),
                                    new String[]{},
                                    configurableCtx,
                                    null
                            )
                    );

                    verify(scheduler, times(1)).schedule();
                });
    }

    @Test
    @DisplayName("IT should work with no schedulers and no metrics beans")
    void shouldWorkWithNoSchedulersAndNoMetricsBeans() {
        contextRunner.run(ctx ->
                assertThatCode(() ->
                        ctx.getBean(PostApplicationStartOutboxInitializer.class).init()
                ).doesNotThrowAnyException()
        );
    }

    @Test
    @DisplayName("IT should not call schedule twice when init called twice")
    void shouldCallScheduleOncePerInvocation() {
        contextRunner
                .withBean("testScheduler", OutboxScheduler.class, () -> mock(OutboxScheduler.class))
                .run(ctx -> {
                    OutboxScheduler scheduler = ctx.getBean("testScheduler", OutboxScheduler.class);
                    PostApplicationStartOutboxInitializer initializer =
                            ctx.getBean(PostApplicationStartOutboxInitializer.class);

                    initializer.init();
                    initializer.init();

                    verify(scheduler, times(2)).schedule();
                });
    }
}
