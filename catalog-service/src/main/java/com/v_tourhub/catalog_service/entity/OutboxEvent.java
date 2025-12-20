package com.v_tourhub.catalog_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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