package com.v_tourhub.booking_service.entity;

public enum BookingStatus {
    PENDING_PAYMENT, // Chờ thanh toán
    CONFIRMED,       // Đã thanh toán & giữ chỗ thành công
    COMPLETED,       // Đã check-in/sử dụng dịch vụ
    CANCELLED,       // Hủy
    REFUNDED         // Đã hoàn tiền
}