package com.v_tourhub.catalog_service.entity;

import com.soa.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tourism_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourismService extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal price;
    
    private Boolean availability;
    
    private Long providerId; // ID của Provider (Service khác)

    @Enumerated(EnumType.STRING)
    private ServiceType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id")
    @JsonIgnore
    private Destination destination;
    
    // Rating riêng cho dịch vụ
    private Double averageRating;

    public enum ServiceType {
        HOTEL, RESTAURANT, ATTRACTION, TOUR
    }
}