package com.v_tourhub.userprofile_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.v_tourhub.userprofile_service.entity.UserProfile;
import com.v_tourhub.userprofile_service.repository.UserProfileRepository;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository profileRepo;
    private final StorageService storageService;

    public UserProfile getOrCreateProfile(String userId, String email, String firstName, String lastName) {
        return profileRepo.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfile newProfile = UserProfile.builder()
                            .userId(userId)
                            .email(email)
                            .firstName(firstName)
                            .lastName(lastName)
                            .build();
                    return profileRepo.save(newProfile);
                });
    }

    public UserProfile updateProfile(String userId, UserProfile request) {
        UserProfile profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        
        profile.setBio(request.getBio());
        profile.setPhone(request.getPhone());
        profile.setAddress(request.getAddress());
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        
        return profileRepo.save(profile);
    }

    @Transactional
    public String uploadAvatar(String userId, MultipartFile file) {
        UserProfile profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        
        String fileName = storageService.uploadFile(file);
        
        profile.setAvatarUrl(fileName);
        profileRepo.save(profile);
        
        return storageService.getFileUrl(fileName);
    }
    
    public UserProfile getProfileWithAvatar(String userId) {
        UserProfile profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().startsWith("http")) {
            profile.setAvatarUrl(storageService.getFileUrl(profile.getAvatarUrl()));
        }
        return profile;
    }
}
