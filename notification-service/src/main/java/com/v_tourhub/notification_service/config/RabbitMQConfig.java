package com.v_tourhub.notification_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    public static final String QUEUE_CANCELLATION_EMAIL = "notification.cancellation.email.queue";
    public static final String ROUTING_KEY_CANCELLED = "booking.cancelled";

    @Bean
    public Queue cancellationEmailQueue() {
        return new Queue(QUEUE_CANCELLATION_EMAIL, true);
    }
    
    @Bean
    public Binding bindingCancellationEmail(Queue cancellationEmailQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(cancellationEmailQueue).to(bookingExchange).with(ROUTING_KEY_CANCELLED);
    }

    public static final String QUEUE_BOOKING_FAILED_EMAIL = "notification.booking.failed.email.queue";
    public static final String ROUTING_KEY_BOOKING_FAILED = "booking.failed";

    @Bean
    public Queue bookingFailedEmailQueue() {
        return new Queue(QUEUE_BOOKING_FAILED_EMAIL, true);
    }
    
    @Bean
    public Binding bindingBookingFailedEmail(Queue bookingFailedEmailQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(bookingFailedEmailQueue).to(bookingExchange).with(ROUTING_KEY_BOOKING_FAILED);
    }
}
