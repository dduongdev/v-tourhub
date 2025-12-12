package com.v_tourhub.userprofile_service.entity;

import com.soa.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String userId; 

    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    
    @Column(columnDefinition = "TEXT")
    private String bio;
    
    private String avatarUrl;
    
    private String address;
    private String currency; // VND, USD
}