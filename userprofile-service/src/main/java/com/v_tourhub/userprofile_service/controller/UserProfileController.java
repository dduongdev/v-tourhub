package com.v_tourhub.userprofile_service.controller;

import com.soa.common.dto.ApiResponse;
import com.v_tourhub.userprofile_service.entity.UserProfile;
import com.v_tourhub.userprofile_service.service.UserProfileService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;

    @GetMapping("/profile")
    public ApiResponse<UserProfile> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("sub");
        String email = jwt.getClaimAsString("email"); 
        String first = jwt.getClaimAsString("given_name");
        String last = jwt.getClaimAsString("family_name");

        return ApiResponse.success(profileService.getOrCreateProfile(userId, email, first, last));
    }
    
    @GetMapping("/{userId}/public")
    public ApiResponse<UserProfile> getPublicProfile(@PathVariable String userId) {
        return ApiResponse.success(profileService.getProfileWithAvatar(userId));
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfile> updateProfile(@AuthenticationPrincipal Jwt jwt, @RequestBody UserProfile request) {
        String userId = jwt.getClaimAsString("sub");
        return ApiResponse.success(profileService.updateProfile(userId, request));
    }

    @PostMapping("/avatar")
    public ApiResponse<String> uploadAvatar(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file) {
        String userId = jwt.getClaimAsString("sub");
        String url = profileService.uploadAvatar(userId, file);
        return ApiResponse.success(url);
    }
}