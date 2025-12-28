package com.v_tourhub.booking_service.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponse {
    private Long bookingId;
    private String status;
    private Long serviceId;
    private String serviceName;
    private BigDecimal totalPrice;
    private LocalDateTime expiresAt;
    private boolean isPaymentReady;
    private String paymentUrl;
}