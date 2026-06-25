package com.loveradar.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private com.loveradar.dto.session.SessionResponse activeSession;
    private String partnerStatus;
    private List<com.loveradar.dto.alert.AlertResponse> recentAlerts;
    private int radiusMeters;
    private Long sessionDurationSeconds;
    private List<EncounterInsight> topEncounters;
    private List<String> aiInsights;
    private List<HeatmapPoint> heatmap;
}
