package com.soa.payment_service.entity;

public enum PaymentStatus {
    CONFIRMED,  // Đã xác nhận (chờ thanh toán)
    COMPLETED,  // Thanh toán thành công
    FAILED,     // Thanh toán thất bại
    REFUNDED,   // Đã hoàn tiền
    CANCELLED   // Đã hủy
}