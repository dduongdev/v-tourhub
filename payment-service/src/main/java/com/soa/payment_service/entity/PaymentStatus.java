package com.soa.payment_service.entity;

public enum PaymentStatus {
    PENDING,    // Mới tạo từ booking
    COMPLETED,  // Thanh toán thành công
    FAILED,     // Thanh toán thất bại
    REFUNDED    // Đã hoàn tiền
}