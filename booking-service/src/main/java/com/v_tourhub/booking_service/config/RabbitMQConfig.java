package com.v_tourhub.booking_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "booking.exchange";
    public static final String QUEUE_PAYMENT = "payment.process.queue";
    public static final String ROUTING_KEY_CREATED = "booking.created";

    public static final String QUEUE_CANCELLED = "booking.cancelled.queue";
    public static final String ROUTING_KEY_CANCELLED = "booking.cancelled";

    public static final String QUEUE_PAYMENT_COMPLETED = "payment.completed.queue";
    public static final String ROUTING_KEY_PAYMENT_COMPLETED = "payment.completed";

    public static final String QUEUE_PAYMENT_FAILED = "payment.failed.queue";
    public static final String ROUTING_KEY_PAYMENT_FAILED = "payment.failed";
    public static final String ROUTING_KEY_REFUND_REQUESTED = "refund.requested";

    public static final String ROUTING_KEY_CONFIRMED = "booking.confirmed";

    public static final String QUEUE_INVENTORY_LOCK_FAILED = "booking.inventory.lock.failed.queue";
    public static final String ROUTING_KEY_INVENTORY_LOCK_FAILED = "inventory.lock.failed";
    public static final String ROUTING_KEY_READY_FOR_PAYMENT = "booking.ready_for_payment";

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
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

    @Bean
    public Queue paymentFailedQueue() {
        return new Queue(QUEUE_PAYMENT_FAILED, true);
    }

    @Bean
    public Binding bindingPaymentFailed(Queue paymentFailedQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(paymentFailedQueue).to(bookingExchange).with(ROUTING_KEY_PAYMENT_FAILED);
    }

    @Bean
    public Queue inventoryLockFailedQueue() {
        return new Queue(QUEUE_INVENTORY_LOCK_FAILED, true);
    }

    @Bean
    public Binding bindingInventoryLockFailed(Queue inventoryLockFailedQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(inventoryLockFailedQueue).to(bookingExchange)
                .with(ROUTING_KEY_INVENTORY_LOCK_FAILED);
    }

    public static final String QUEUE_BOOKING_FAILED_PAYMENT = "payment.booking.failed.queue";
    public static final String ROUTING_KEY_BOOKING_FAILED = "booking.failed";

    @Bean
    public Queue bookingFailedQueueForPayment() {
        return new Queue(QUEUE_BOOKING_FAILED_PAYMENT, true);
    }

    @Bean
    public Binding bindingBookingFailedForPayment(Queue bookingFailedQueueForPayment, TopicExchange exchange) {
        return BindingBuilder.bind(bookingFailedQueueForPayment).to(exchange).with(ROUTING_KEY_BOOKING_FAILED);
    }

    public static final String QUEUE_INVENTORY_LOCK_SUCCESSFUL = "booking.inventory.lock.successful.queue";
    public static final String ROUTING_KEY_INVENTORY_LOCK_SUCCESSFUL = "inventory.lock.successful";

    @Bean
    public Queue inventoryLockSuccessfulQueue() {
        return new Queue(QUEUE_INVENTORY_LOCK_SUCCESSFUL, true);
    }

    @Bean
    public Binding bindingInventoryLockSuccessful(Queue inventoryLockSuccessfulQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(inventoryLockSuccessfulQueue).to(bookingExchange)
                .with(ROUTING_KEY_INVENTORY_LOCK_SUCCESSFUL);
    }
}