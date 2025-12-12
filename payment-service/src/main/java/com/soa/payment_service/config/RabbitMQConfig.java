package com.soa.payment_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "booking.exchange";

    public static final String QUEUE_PAYMENT_PROCESS = "payment.process.queue";
    public static final String ROUTING_KEY_BOOKING_CREATED = "booking.created";

    public static final String ROUTING_KEY_PAYMENT_COMPLETED = "payment.completed";
    public static final String ROUTING_KEY_PAYMENT_FAILED = "payment.failed";

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue paymentProcessQueue() {
        return new Queue(QUEUE_PAYMENT_PROCESS, true);
    }

    @Bean
    public Binding bindingPaymentProcess(Queue paymentProcessQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(paymentProcessQueue).to(bookingExchange).with(ROUTING_KEY_BOOKING_CREATED);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}