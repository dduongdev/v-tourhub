package com.v_tourhub.booking_service.entity;

import com.soa.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    @Column(nullable = false)
    private String userId; 

    @Column(nullable = false)
    private Long serviceId;

    private String serviceName;

    private Long providerId; 

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private LocalDate checkInDate;
    
    private LocalDate checkOutDate;
    
    private Integer guests;

    private BigDecimal totalPrice;

    private String inventoryLockToken;
    
    private LocalDateTime expiresAt
;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
}