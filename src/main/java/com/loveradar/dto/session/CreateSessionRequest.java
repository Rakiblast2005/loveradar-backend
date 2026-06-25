package com.loveradar.dto.session;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSessionRequest {

    /**
     * Allowed values: 100, 250, 500, 1000 (meters)
     */
    @NotNull(message = "Radius is required")
    private Integer radiusMeters;
}
