package com.loveradar.dto.session;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRadiusRequest {

    @NotNull(message = "Radius is required")
    private Integer radiusMeters;
}
