package com.loveradar.service;

import com.loveradar.dto.alert.AlertResponse;
import com.loveradar.dto.dashboard.DashboardResponse;
import com.loveradar.dto.dashboard.EncounterInsight;
import com.loveradar.dto.dashboard.HeatmapPoint;
import com.loveradar.entity.Alert;
import com.loveradar.entity.AlertType;
import com.loveradar.entity.CoupleSession;
import com.loveradar.entity.EncounterHistory;
import com.loveradar.entity.User;
import com.loveradar.repository.AlertRepository;
import com.loveradar.repository.EncounterHistoryRepository;
import com.loveradar.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserService userService;
    private final SessionService sessionService;
    private final SessionRepository sessionRepository;
    private final AlertService alertService;
    private final AlertRepository alertRepository;
    private final EncounterHistoryRepository encounterHistoryRepository;
    private final LocationService locationService;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        User user = userService.getCurrentUser();

        var activeSessionOpt = sessionRepository.findActiveSessionForUser(user);

        List<AlertResponse> recentAlerts;
        String partnerStatus = "NOT_JOINED";
        int radius = user.getDefaultRadiusMeters();
        Long durationSeconds = null;
        com.loveradar.dto.session.SessionResponse sessionResponse = null;

        if (activeSessionOpt.isPresent()) {
            CoupleSession session = activeSessionOpt.get();
            sessionResponse = sessionService.toResponse(session);
            if (session.isEmergencyHidden()) {
                // Emergency mode: hide session details from the dashboard payload
                sessionResponse = null;
                recentAlerts = List.of();
            } else {
                recentAlerts = alertService.getRecentAlertsForSession(session, 10);
            }
            radius = session.getRadiusMeters();
            durationSeconds = sessionService.toResponse(session).getDurationSeconds();
            partnerStatus = locationService.resolvePartnerStatus(session, user);
        } else {
            recentAlerts = alertService.getAlertsForCurrentUser().stream().limit(10).toList();
        }

        return DashboardResponse.builder()
                .activeSession(sessionResponse)
                .partnerStatus(partnerStatus)
                .recentAlerts(recentAlerts)
                .radiusMeters(radius)
                .sessionDurationSeconds(durationSeconds)
                .topEncounters(getTopEncounters(user))
                .aiInsights(generateAiInsights(user))
                .heatmap(getHeatmap(user))
                .build();
    }

    private List<EncounterInsight> getTopEncounters(User user) {
        return encounterHistoryRepository.findByUserOrderByEncounterCountDesc(user).stream()
                .limit(5)
                .map(history -> EncounterInsight.builder()
                        .contactName(history.getContact().getContactName())
                        .encounterCount(history.getEncounterCount())
                        .lastEncountered(history.getLastEncountered() != null
                                ? history.getLastEncountered().format(ISO) : null)
                        .build())
                .toList();
    }

    /**
     * Generates simple, rule-based "AI insight" sentences from encounter
     * history. This intentionally avoids any external AI service call so
     * the feature works fully offline and deterministically; it can be
     * swapped for an LLM-backed summary later without changing the contract.
     */
    private List<String> generateAiInsights(User user) {
        List<String> insights = new ArrayList<>();
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();

        List<Alert> enterAlertsThisMonth = alertRepository.findForUserSince(user, AlertType.ENTER, monthStart);

        Map<String, Long> countsByContact = new HashMap<>();
        for (Alert alert : enterAlertsThisMonth) {
            countsByContact.merge(alert.getContactName(), 1L, Long::sum);
        }

        if (countsByContact.isEmpty()) {
            insights.add("No proximity encounters recorded yet this month. Start a session to begin tracking.");
        } else {
            for (Map.Entry<String, Long> entry : countsByContact.entrySet()) {
                insights.add(String.format("You crossed paths with %s %d time%s this month.",
                        entry.getKey(), entry.getValue(), entry.getValue() == 1 ? "" : "s"));
            }
        }

        long totalEncounters = encounterHistoryRepository.findByUserOrderByEncounterCountDesc(user).stream()
                .mapToLong(EncounterHistory::getEncounterCount)
                .sum();

        if (totalEncounters >= 10) {
            insights.add("You have a busy social radius — consider reviewing your trusted contact list.");
        }

        return insights;
    }

    /**
     * Builds a privacy-preserving heatmap by grouping past ENTER alerts
     * into coarse ~1.1km grid cells and counting frequency per cell.
     * Exact coordinates and routes are never exposed.
     */
    private List<HeatmapPoint> getHeatmap(User user) {
        List<Alert> alerts = alertRepository.findAllForUser(user).stream()
                .filter(a -> a.getAlertType() == AlertType.ENTER)
                .filter(a -> a.getApproxLatCell() != null && a.getApproxLngCell() != null)
                .toList();

        Map<String, HeatmapPoint> cells = new HashMap<>();
        for (Alert alert : alerts) {
            String key = alert.getApproxLatCell() + ":" + alert.getApproxLngCell();
            HeatmapPoint existing = cells.get(key);
            if (existing == null) {
                cells.put(key, HeatmapPoint.builder()
                        .latCell(alert.getApproxLatCell())
                        .lngCell(alert.getApproxLngCell())
                        .count(1)
                        .build());
            } else {
                cells.put(key, HeatmapPoint.builder()
                        .latCell(existing.getLatCell())
                        .lngCell(existing.getLngCell())
                        .count(existing.getCount() + 1)
                        .build());
            }
        }
        return new ArrayList<>(cells.values());
    }
}
