package com.v_tourhub.review_service.config;

import com.soa.common.util.AppConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange cho Review events
    @Bean
    public TopicExchange reviewExchange() {
        return new TopicExchange(AppConstants.EXCHANGE_REVIEW);
    }

    // Queue cho Review Created
    @Bean
    public Queue reviewCreatedQueue() {
        return QueueBuilder.durable(AppConstants.QUEUE_REVIEW_CREATED).build();
    }

    // Queue cho Review Updated
    @Bean
    public Queue reviewUpdatedQueue() {
        return QueueBuilder.durable(AppConstants.QUEUE_REVIEW_UPDATED).build();
    }

    // Queue cho Review Deleted
    @Bean
    public Queue reviewDeletedQueue() {
        return QueueBuilder.durable(AppConstants.QUEUE_REVIEW_DELETED).build();
    }

    // Bindings
    @Bean
    public Binding reviewCreatedBinding() {
        return BindingBuilder
                .bind(reviewCreatedQueue())
                .to(reviewExchange())
                .with(AppConstants.ROUTING_KEY_REVIEW_CREATED);
    }

    @Bean
    public Binding reviewUpdatedBinding() {
        return BindingBuilder
                .bind(reviewUpdatedQueue())
                .to(reviewExchange())
                .with(AppConstants.ROUTING_KEY_REVIEW_UPDATED);
    }

    @Bean
    public Binding reviewDeletedBinding() {
        return BindingBuilder
                .bind(reviewDeletedQueue())
                .to(reviewExchange())
                .with(AppConstants.ROUTING_KEY_REVIEW_DELETED);
    }

    // Message converter để serialize/deserialize JSON
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}

