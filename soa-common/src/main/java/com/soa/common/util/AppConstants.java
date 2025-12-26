package com.soa.common.util;

public class AppConstants {
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String PAGE_NUMBER_DEFAULT = "0";
    public static final String PAGE_SIZE_DEFAULT = "10";
    
    // RabbitMQ Exchanges & Queues
    public static final String EXCHANGE_BOOKING = "booking.exchange";
    public static final String QUEUE_BOOKING_CREATED = "booking.created.queue";
    public static final String ROUTING_KEY_BOOKING_CREATED = "booking.created";
    
    // Review Events
    public static final String EXCHANGE_REVIEW = "review.exchange";
    public static final String QUEUE_REVIEW_CREATED = "review.created.queue";
    public static final String QUEUE_REVIEW_UPDATED = "review.updated.queue";
    public static final String QUEUE_REVIEW_DELETED = "review.deleted.queue";
    public static final String ROUTING_KEY_REVIEW_CREATED = "review.created";
    public static final String ROUTING_KEY_REVIEW_UPDATED = "review.updated";
    public static final String ROUTING_KEY_REVIEW_DELETED = "review.deleted";

    private AppConstants() {} 
}