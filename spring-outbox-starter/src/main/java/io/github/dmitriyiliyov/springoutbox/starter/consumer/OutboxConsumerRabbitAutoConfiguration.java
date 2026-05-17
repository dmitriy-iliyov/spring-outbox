package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "outbox.consumer",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnProperty(
        prefix = "outbox.consumer.source",
        name = "type",
        havingValue = "rabbit"
)
public class OutboxConsumerRabbitAutoConfiguration {

    private final OutboxConsumerProperties consumerProperties;

    public OutboxConsumerRabbitAutoConfiguration(OutboxConsumerProperties consumerProperties) {
        this.consumerProperties = consumerProperties;
    }

    @Bean(name = "outboxRabbitMessageConverter")
    @ConditionalOnMissingBean(name = "outboxRabbitMessageConverter")
    public MessageConverter outboxRabbitMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setClassMapper(new OutboxRabbitClassMapper(consumerProperties.getMappings()));
        return converter;
    }

    @Bean(name = "outboxRabbitListenerContainerFactory")
    @ConditionalOnMissingBean(name = "outboxRabbitListenerContainerFactory")
    public RabbitListenerContainerFactory<?> outboxRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            @Qualifier("outboxRabbitMessageConverter") MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}
