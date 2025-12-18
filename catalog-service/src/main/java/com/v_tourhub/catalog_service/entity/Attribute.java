package com.v_tourhub.catalog_service.entity;

import com.soa.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attributes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attribute extends BaseEntity {

    @Column(nullable = false)
    private String attributeKey;

    @Column(nullable = false)
    private String attributeValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tourism_service_id")
    @JsonIgnore
    private TourismService tourismService;
}