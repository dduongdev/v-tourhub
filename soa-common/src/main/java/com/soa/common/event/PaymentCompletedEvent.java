package com.soa.common.event;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentCompletedEvent {
    private Long bookingId;
    private String transactionId;
}