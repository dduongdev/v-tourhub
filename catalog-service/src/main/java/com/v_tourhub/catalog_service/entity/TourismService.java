package com.v_tourhub.catalog_service.entity;

import com.soa.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

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
    
    private Long providerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id")
    @JsonIgnore
    private Destination destination;

    @OneToMany(mappedBy = "tourismService", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Media> mediaList;

    @OneToMany(mappedBy = "tourismService", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attribute> attributes;

    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;

    public enum ServiceType {
        HOTEL, // Bán theo đêm (Date Range)
        TOUR,  // Bán theo suất/ngày khởi hành (Single Date)
        ACTIVITY, // Vé tham quan, vé xe (Single Date)
        RESTAURANT // Có thể không cần kho, hoặc kho theo bàn/giờ
    }
}