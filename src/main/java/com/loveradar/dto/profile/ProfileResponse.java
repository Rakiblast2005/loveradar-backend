package com.loveradar.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private String id;
    private String name;
    private String email;
    private String profilePictureUrl;
    private boolean shareLocation;
    private int defaultRadiusMeters;
    private boolean darkMode;
    private String createdAt;
}
