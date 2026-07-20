package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.springoutbox.core.polling.OutboxScheduleStrategy;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxPollingScheduler;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxProcessor;
import io.github.dmitriyiliyov.springoutbox.starter.*;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.concurrent.ScheduledExecutorService;


public class OutboxPollingSchedulerRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware {

    private Environment environment;
    private BeanFactory beanFactory;

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {

        Binder binder = Binder.get(environment);
        OutboxPublisherProperties properties = binder.bind(
                "outbox",
                Bindable.of(OutboxProperties.class)
                ).orElseThrow(() -> new IllegalStateException("outbox properties not specified"))
                .getPublisher();
        properties.applyDefaults();

        for (OutboxPublisherProperties.EventProperties event : properties.getEvents().values()) {

            String beanName = BeanNameUtils.toBeanName(
                    event.getEventType(),
                    "OutboxPollingScheduler"
            );

            RootBeanDefinition definition = new RootBeanDefinition(OutboxPollingScheduler.class);
            definition.setInstanceSupplier(() -> {

                ScheduledExecutorService executor = beanFactory.getBean(
                        "outboxScheduledExecutorService",
                        ScheduledExecutorService.class
                );

                OutboxProcessor processor = beanFactory.getBean(OutboxProcessor.class);

                OutboxScheduleStrategyListenerSupplier listenerSupplier = beanFactory.getBean(
                        OutboxScheduleStrategyListenerSupplier.class
                );

                ContinuableTaskDecoratorSupplier decoratorSupplier = beanFactory.getBean(
                        ContinuableTaskDecoratorSupplier.class
                );

                OutboxScheduleStrategy strategy = OutboxScheduleStrategyFactory.create(
                        event.getEventType(),
                        event.getPolling(),
                        executor,
                        listenerSupplier
                );

                ContinuableTaskDecorator decorator = decoratorSupplier.supply(event.getEventType());

                return new OutboxPollingScheduler(event, strategy, processor, decorator);
            });

            registry.registerBeanDefinition(beanName,definition);
        }
    }
}