package com.v_tourhub.catalog_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "booking.exchange";

    // --- Queues for Inventory ---
    public static final String QUEUE_BOOKING_CREATED = "catalog.booking.created.queue";
    public static final String QUEUE_BOOKING_CONFIRMED = "catalog.booking.confirmed.queue";
    public static final String QUEUE_BOOKING_CANCELLED = "catalog.booking.cancelled.queue";
    
    // --- Routing Keys ---
    public static final String ROUTING_KEY_CREATED = "booking.created";
    public static final String ROUTING_KEY_CONFIRMED = "booking.confirmed";
    public static final String ROUTING_KEY_CANCELLED = "booking.cancelled";
    public static final String ROUTING_KEY_INVENTORY_LOCK_FAILED = "inventory.lock.failed";

    public static final String ROUTING_KEY_INVENTORY_LOCK_SUCCESSFUL = "inventory.lock.successful";

    @Bean public TopicExchange bookingExchange() { return new TopicExchange(EXCHANGE); }
    @Bean public Queue createdQueue() { return new Queue(QUEUE_BOOKING_CREATED, true); }
    @Bean public Queue confirmedQueue() { return new Queue(QUEUE_BOOKING_CONFIRMED, true); }
    @Bean public Queue cancelledQueue() { return new Queue(QUEUE_BOOKING_CANCELLED, true); }

    @Bean public Binding bindingCreated(Queue createdQueue, TopicExchange exchange) {
        return BindingBuilder.bind(createdQueue).to(exchange).with(ROUTING_KEY_CREATED);
    }
    @Bean public Binding bindingConfirmed(Queue confirmedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(confirmedQueue).to(exchange).with(ROUTING_KEY_CONFIRMED);
    }
    @Bean public Binding bindingCancelled(Queue cancelledQueue, TopicExchange exchange) {
        return BindingBuilder.bind(cancelledQueue).to(exchange).with(ROUTING_KEY_CANCELLED);
    }
    
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}