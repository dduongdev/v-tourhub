package com.soa.common.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingReadyForPaymentEvent {
    private Long bookingId;
    private String serviceName;
    private LocalDate checkIn;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String customerEmail;
    private LocalDateTime expiresAt;
}