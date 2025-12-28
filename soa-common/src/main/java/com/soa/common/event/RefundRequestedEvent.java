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
public class RefundRequestedEvent {
    private Long bookingId;
    private String userId;
    private BigDecimal refundAmount;
    private String reason;
    private String customerEmail;
}
