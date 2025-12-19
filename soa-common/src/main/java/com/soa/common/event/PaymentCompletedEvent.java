package com.soa.common.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private Long bookingId;
    private Long paymentId;
    private String userId;
    private BigDecimal amount;
    private String transactionId; 
    private String paymentMethod;
}