package com.loveradar.dto.session;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinSessionRequest {

    @NotBlank(message = "Session code is required")
    private String sessionCode;
}
