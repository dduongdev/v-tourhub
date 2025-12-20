package com.v_tourhub.catalog_service.entity;

import com.soa.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "inventory_reservations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"booking_id", "date"}) 
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservation extends BaseEntity {

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(nullable = false)
    private LocalDate date;

    private int quantity;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status; 

    public enum ReservationStatus {
        LOCKED, COMMITTED, CANCELLED
    }
}