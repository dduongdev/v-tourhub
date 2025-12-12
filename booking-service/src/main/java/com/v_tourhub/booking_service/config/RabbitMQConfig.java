package com.v_tourhub.booking_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "booking.exchange";
    public static final String QUEUE_PAYMENT = "payment.process.queue";
    public static final String ROUTING_KEY_CREATED = "booking.created";

    public static final String QUEUE_CANCELLED = "booking.cancelled.queue"; 
    public static final String ROUTING_KEY_CANCELLED = "booking.cancelled";

    public static final String QUEUE_PAYMENT_COMPLETED = "payment.completed.queue";
    public static final String ROUTING_KEY_PAYMENT_COMPLETED = "payment.completed";

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public Queue paymentQueue() {
        return new Queue(QUEUE_PAYMENT, true);
    }
    
    @Bean
    public Binding bindingPayment(Queue paymentQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(paymentQueue).to(bookingExchange).with(ROUTING_KEY_CREATED);
    }

    @Bean
    public Queue cancelledQueue() {
        return new Queue(QUEUE_CANCELLED, true);
    }

    @Bean
    public Binding bindingCancelled(Queue cancelledQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(cancelledQueue).to(bookingExchange).with(ROUTING_KEY_CANCELLED);
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return new Queue(QUEUE_PAYMENT_COMPLETED, true);
    }

    @Bean
    public Binding bindingPaymentCompleted(Queue paymentCompletedQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(paymentCompletedQueue).to(bookingExchange).with(ROUTING_KEY_PAYMENT_COMPLETED);
    }
}