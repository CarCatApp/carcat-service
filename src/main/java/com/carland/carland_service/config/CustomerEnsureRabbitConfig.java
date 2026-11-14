package com.carland.carland_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@EnableConfigurationProperties(CustomerEnsureRabbitProperties.class)
public class CustomerEnsureRabbitConfig {

    @Bean
    DirectExchange customerEnsureExchange(CustomerEnsureRabbitProperties props) {
        return new DirectExchange(props.getExchange(), true, false);
    }

    @Bean
    Queue customerEnsureWorkQueue(CustomerEnsureRabbitProperties props) {
        return QueueBuilder.durable(props.getQueue()).build();
    }

    /**
     * Failed ensure messages wait here {@code retryDelayMs}, then dead-letter back to the work queue.
     */
    @Bean
    Queue customerEnsureRetryQueue(CustomerEnsureRabbitProperties props) {
        return QueueBuilder.durable(props.getRetryQueue())
                .withArgument("x-message-ttl", props.getRetryDelayMs())
                .withArgument("x-dead-letter-exchange", props.getExchange())
                .withArgument("x-dead-letter-routing-key", props.getRoutingKey())
                .build();
    }

    @Bean
    Queue customerEnsureDlq(CustomerEnsureRabbitProperties props) {
        return QueueBuilder.durable(props.getDlq()).build();
    }

    @Bean
    Binding customerEnsureWorkBinding(Queue customerEnsureWorkQueue,
                                      DirectExchange customerEnsureExchange,
                                      CustomerEnsureRabbitProperties props) {
        return BindingBuilder.bind(customerEnsureWorkQueue)
                .to(customerEnsureExchange)
                .with(props.getRoutingKey());
    }

    @Bean
    Binding customerEnsureRetryBinding(Queue customerEnsureRetryQueue,
                                       DirectExchange customerEnsureExchange,
                                       CustomerEnsureRabbitProperties props) {
        return BindingBuilder.bind(customerEnsureRetryQueue)
                .to(customerEnsureExchange)
                .with(props.getRetryRoutingKey());
    }

    @Bean
    Binding customerEnsureDlqBinding(Queue customerEnsureDlq,
                                     DirectExchange customerEnsureExchange,
                                     CustomerEnsureRabbitProperties props) {
        return BindingBuilder.bind(customerEnsureDlq)
                .to(customerEnsureExchange)
                .with(props.getDlqRoutingKey());
    }
}
