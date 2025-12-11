package com.soa.common.util;

public class AppConstants {
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String PAGE_NUMBER_DEFAULT = "0";
    public static final String PAGE_SIZE_DEFAULT = "10";
    
    // RabbitMQ Exchanges & Queues
    public static final String EXCHANGE_BOOKING = "booking.exchange";
    public static final String QUEUE_BOOKING_CREATED = "booking.created.queue";
    public static final String ROUTING_KEY_BOOKING_CREATED = "booking.created";

    private AppConstants() {} 
}