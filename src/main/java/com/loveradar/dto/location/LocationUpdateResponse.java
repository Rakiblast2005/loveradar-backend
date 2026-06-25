package com.loveradar.dto.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateResponse {
    private String status;
    private String partnerStatus;
    private List<com.loveradar.dto.alert.AlertResponse> newAlerts;
}
