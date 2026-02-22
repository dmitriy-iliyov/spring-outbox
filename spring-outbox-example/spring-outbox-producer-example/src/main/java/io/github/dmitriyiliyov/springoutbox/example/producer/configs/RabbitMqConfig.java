package io.github.dmitriyiliyov.springoutbox.example.producer.configs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(prefix = "outbox.publisher.sender", name = "type", havingValue = "rabbit_mq")
@Configuration
@Slf4j
public class RabbitMqConfig {

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost("outbox-rabbitmq");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        return factory;
    }

    @Bean
    public RabbitTemplate.ReturnsCallback returnCallback() {
        return returned -> log.error(
                "Message returned. exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText()
        );
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         RabbitTemplate.ReturnsCallback returnCallback) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        template.setReturnsCallback(returnCallback);
        return template;
    }

    @Bean
    public Queue ordersQueue() {
        return new Queue("orders", true);
    }

    @Bean
    public Queue ordersCreatedQueue() {
        return new Queue("orders.created", true);
    }

    @Bean
    public DirectExchange ordersExchange() {
        return new DirectExchange("orders-exchange");
    }

    @Bean
    public Binding ordersUpdateBinding(Queue ordersQueue, DirectExchange ordersExchange) {
        return BindingBuilder
                .bind(ordersQueue)
                .to(ordersExchange)
                .with("update-order");
    }

    @Bean
    public Binding ordersDeleteBinding(Queue ordersQueue, DirectExchange ordersExchange) {
        return BindingBuilder
                .bind(ordersQueue)
                .to(ordersExchange)
                .with("delete-order");
    }

    @Bean
    public Binding ordersCreatedBinding(Queue ordersCreatedQueue, DirectExchange ordersExchange) {
        return BindingBuilder
                .bind(ordersCreatedQueue)
                .to(ordersExchange)
                .with("create-order");
    }
}
