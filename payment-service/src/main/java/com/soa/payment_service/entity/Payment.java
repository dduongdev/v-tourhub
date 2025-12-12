package com.soa.payment_service.entity;

import com.soa.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Column(nullable = false)
    private Long bookingId;

    private String userId; 

    @Column(nullable = false)
    private BigDecimal amount;

    private String currency; // VND, USD

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String gatewayTransactionId; 
}