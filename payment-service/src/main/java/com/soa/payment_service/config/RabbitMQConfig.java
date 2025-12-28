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

    public static final String ROUTING_KEY_PAYMENT_COMPLETED = "payment.completed";
    public static final String ROUTING_KEY_PAYMENT_FAILED = "payment.failed";

    public static final String QUEUE_BOOKING_CANCELLED = "payment.booking.cancelled.queue";
    public static final String ROUTING_KEY_BOOKING_CANCELLED = "booking.cancelled";

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(EXCHANGE);
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

    public static final String ROUTING_KEY_PAYMENT_INITIATED = "payment.initiated";

    public static final String QUEUE_READY_FOR_PAYMENT = "payment.ready_for_payment.queue";
    public static final String ROUTING_KEY_READY_FOR_PAYMENT = "booking.ready_for_payment";

    @Bean
    public Queue readyForPaymentQueue() {
        return new Queue(QUEUE_READY_FOR_PAYMENT, true);
    }

    @Bean
    public Binding bindingReadyForPayment(Queue readyForPaymentQueue, TopicExchange exchange) {
        return BindingBuilder.bind(readyForPaymentQueue).to(exchange).with(ROUTING_KEY_READY_FOR_PAYMENT);
    }

    // Refund queues and routing keys
    public static final String QUEUE_REFUND_REQUESTED = "payment.refund.requested.queue";
    public static final String ROUTING_KEY_REFUND_REQUESTED = "refund.requested";
    public static final String ROUTING_KEY_REFUND_COMPLETED = "refund.completed";

    @Bean
    public Queue refundRequestedQueue() {
        return new Queue(QUEUE_REFUND_REQUESTED, true);
    }

    @Bean
    public Binding bindingRefundRequested(Queue refundRequestedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(refundRequestedQueue).to(exchange).with(ROUTING_KEY_REFUND_REQUESTED);
    }
}
