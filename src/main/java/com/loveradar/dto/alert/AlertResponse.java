package com.loveradar.dto.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private String id;
    private String sessionId;
    private String contactName;
    private String alertType;   // ENTER or EXIT
    private String alertLevel;  // INFO, WARNING, CRITICAL
    private String message;
    private String approximateDistance;
    private String createdAt;
}
