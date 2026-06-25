package com.loveradar.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    private String id;
    private String sessionCode;
    private String creatorName;
    private String partnerName;
    private boolean partnerJoined;
    private int radiusMeters;
    private String startTime;
    private String endTime;
    private boolean active;
    private boolean emergencyHidden;
    private Long durationSeconds;
}
