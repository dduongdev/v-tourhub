package com.v_tourhub.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType; // VD: "Booking"

    @Column(nullable = false)
    private String aggregateId;   // VD: "17" (bookingId)

    @Column(nullable = false)
    private String eventType;     // VD: "booking.created"

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;       // Nội dung event (JSON)

    @Builder.Default    
    private boolean isPublished = false;

    @Builder.Default 
    private LocalDateTime createdAt = LocalDateTime.now();
}