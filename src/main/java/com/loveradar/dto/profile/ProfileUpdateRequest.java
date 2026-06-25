package com.loveradar.dto.profile;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {

    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @Size(max = 500)
    private String profilePictureUrl;

    private Boolean shareLocation;

    private Integer defaultRadiusMeters;

    private Boolean darkMode;
}
