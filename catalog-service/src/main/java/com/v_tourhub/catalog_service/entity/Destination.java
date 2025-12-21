package com.v_tourhub.catalog_service.entity;

import com.soa.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "destinations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE destinations SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false") 
public class Destination extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "destination")
    private List<TourismService> services;

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL)
    private List<Media> mediaList;

    private String address;
    private String city;
    private String province;
    private Double latitude;
    private Double longitude;
}