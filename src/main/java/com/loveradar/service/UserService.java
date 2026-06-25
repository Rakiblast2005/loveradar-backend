package com.loveradar.service;

import com.loveradar.dto.profile.ProfileResponse;
import com.loveradar.dto.profile.ProfileUpdateRequest;
import com.loveradar.entity.User;
import com.loveradar.exception.BadRequestException;
import com.loveradar.exception.ResourceNotFoundException;
import com.loveradar.repository.UserRepository;
import com.loveradar.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Resolves the currently authenticated user from the Spring Security context.
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResourceNotFoundException("No authenticated user in context");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public ProfileResponse getProfile() {
        return toProfileResponse(getCurrentUser());
    }

    @Transactional
    public ProfileResponse updateProfile(ProfileUpdateRequest request) {
        User user = getCurrentUser();

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        if (request.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(request.getProfilePictureUrl());
        }
        if (request.getShareLocation() != null) {
            user.setShareLocation(request.getShareLocation());
        }
        if (request.getDefaultRadiusMeters() != null) {
            user.setDefaultRadiusMeters(validateRadius(request.getDefaultRadiusMeters()));
        }
        if (request.getDarkMode() != null) {
            user.setDarkMode(request.getDarkMode());
        }

        user = userRepository.save(user);
        return toProfileResponse(user);
    }

    /**
     * ISSUE #2 FIX: Only ONE updateProfilePicture method. The previous file had
     * three copies with slightly different validation — a direct copy-paste corruption.
     * This single version stores locally; swap the transferTo() call for S3/GCS in prod.
     */
    @Transactional
    public ProfileResponse updateProfilePicture(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file provided");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("File must be an image (JPEG, PNG, WEBP, etc.)");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BadRequestException("Image must be under 5 MB");
        }

        User user = getCurrentUser();

        try {
            Path uploadDir = Paths.get("uploads", "profile-pictures");
            Files.createDirectories(uploadDir);

            String extension = switch (contentType) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> ".jpg";
            };
            String filename = user.getId() + "_" + System.currentTimeMillis() + extension;
            Path target = uploadDir.resolve(filename);
            file.transferTo(target);

            user.setProfilePictureUrl("/uploads/profile-pictures/" + filename);
        } catch (IOException e) {
            throw new BadRequestException("Failed to store uploaded image: " + e.getMessage());
        }

        user = userRepository.save(user);
        return toProfileResponse(user);
    }

    public static int validateRadius(int radiusMeters) {
        if (radiusMeters != 100 && radiusMeters != 250 && radiusMeters != 500 && radiusMeters != 1000) {
            throw new BadRequestException("Radius must be one of: 100, 250, 500, or 1000 meters");
        }
        return radiusMeters;
    }

    private ProfileResponse toProfileResponse(User user) {
        return ProfileResponse.builder()
                .id(user.getId().toString())
                .name(user.getName())
                .email(user.getEmail())
                .profilePictureUrl(user.getProfilePictureUrl())
                .shareLocation(user.isShareLocation())
                .defaultRadiusMeters(user.getDefaultRadiusMeters())
                .darkMode(user.isDarkMode())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(ISO) : null)
                .build();
    }
}
