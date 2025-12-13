package com.v_tourhub.notification_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "booking.exchange";
    
    public static final String QUEUE_EMAIL = "notification.email.queue";
    public static final String ROUTING_KEY_CONFIRMED = "booking.confirmed";

    @Bean
    public Queue emailQueue() {
        return new Queue(QUEUE_EMAIL, true);
    }

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding bindingEmail(Queue emailQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(emailQueue).to(bookingExchange).with(ROUTING_KEY_CONFIRMED);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
