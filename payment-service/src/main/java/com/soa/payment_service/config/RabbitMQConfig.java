package com.soa.payment_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "booking.exchange";

    public static final String QUEUE_PAYMENT_PROCESS = "payment.process.queue";
    public static final String ROUTING_KEY_BOOKING_CREATED = "booking.created";

    public static final String ROUTING_KEY_PAYMENT_COMPLETED = "payment.completed";
    public static final String ROUTING_KEY_PAYMENT_FAILED = "payment.failed";

    public static final String QUEUE_BOOKING_CANCELLED = "payment.booking.cancelled.queue";
    public static final String ROUTING_KEY_BOOKING_CANCELLED = "booking.cancelled";

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
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public Queue bookingCancelledQueue() {
        return new Queue(QUEUE_BOOKING_CANCELLED, true);
    }

    @Bean
    public Binding bindingBookingCancelled(Queue bookingCancelledQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(bookingCancelledQueue).to(bookingExchange).with(ROUTING_KEY_BOOKING_CANCELLED);
    }

    public static final String QUEUE_BOOKING_FAILED = "payment.booking.failed.queue";
    public static final String ROUTING_KEY_BOOKING_FAILED = "booking.failed";

    @Bean
    public Queue bookingFailedQueue() {
        return new Queue(QUEUE_BOOKING_FAILED, true);
    }

    @Bean
    public Binding bindingBookingFailed(Queue bookingFailedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(bookingFailedQueue).to(exchange).with(ROUTING_KEY_BOOKING_FAILED);
    }
}