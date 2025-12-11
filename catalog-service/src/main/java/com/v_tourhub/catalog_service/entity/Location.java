package com.v_tourhub.catalog_service.entity;

import com.soa.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location extends BaseEntity {
    private String address;
    private String city;
    private String province;
    private Double latitude;
    private Double longitude;
}