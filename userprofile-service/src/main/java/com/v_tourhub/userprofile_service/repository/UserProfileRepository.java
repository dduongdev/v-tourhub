package com.v_tourhub.userprofile_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.v_tourhub.userprofile_service.entity.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    Optional<UserProfile> findByUserId(String userId);
}
