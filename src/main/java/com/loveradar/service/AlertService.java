package com.loveradar.service;

import com.loveradar.dto.alert.AlertResponse;
import com.loveradar.entity.Alert;
import com.loveradar.entity.AlertLevel;
import com.loveradar.entity.AlertType;
import com.loveradar.entity.Contact;
import com.loveradar.entity.CoupleSession;
import com.loveradar.entity.User;
import com.loveradar.repository.AlertRepository;
import com.loveradar.util.HaversineUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserService userService;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;

    @Transactional
    public Alert createAlert(CoupleSession session, Contact contact, AlertType type, double distanceMeters,
                              Double approxLat, Double approxLng) {
        AlertLevel level = resolveLevel(type, distanceMeters, session.getRadiusMeters());
        String message = buildMessage(contact, type, distanceMeters);

        Alert alert = Alert.builder()
                .session(session)
                .contact(contact)
                .contactName(contact.getContactName())
                .alertType(type)
                .alertLevel(level)
                .message(message)
                .distance(distanceMeters)
                .approxLatCell(approxLat != null ? HaversineUtil.roundToApproxCell(approxLat) : null)
                .approxLngCell(approxLng != null ? HaversineUtil.roundToApproxCell(approxLng) : null)
                .build();

        return alertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlertsForCurrentUser() {
        User user = userService.getCurrentUser();
        return alertRepository.findAllForUser(user).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getRecentAlertsForSession(CoupleSession session, int limit) {
        return alertRepository.findBySessionOrderByCreatedAtDesc(session).stream()
                .limit(limit)
                .map(this::toResponse)
                .toList();
    }

    public AlertResponse toResponse(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId().toString())
                .sessionId(alert.getSession().getId().toString())
                .contactName(alert.getContactName())
                .alertType(alert.getAlertType().name())
                .alertLevel(alert.getAlertLevel().name())
                .message(alert.getMessage())
                .approximateDistance(HaversineUtil.approximateDistanceLabel(alert.getDistance()))
                .createdAt(alert.getCreatedAt() != null ? alert.getCreatedAt().format(ISO) : null)
                .build();
    }

    private AlertLevel resolveLevel(AlertType type, double distanceMeters, int radiusMeters) {
        if (type == AlertType.EXIT) {
            return AlertLevel.INFO;
        }
        if (distanceMeters <= radiusMeters / 4.0) {
            return AlertLevel.CRITICAL;
        }
        if (distanceMeters <= radiusMeters / 2.0) {
            return AlertLevel.WARNING;
        }
        return AlertLevel.INFO;
    }

    private String buildMessage(Contact contact, AlertType type, double distanceMeters) {
        String roundedDistance = roundDistanceForMessage(distanceMeters);
        if (type == AlertType.ENTER) {
            return String.format("Alert: A contact from your trusted list is within %s meters.", roundedDistance);
        }
        return String.format("Update: A trusted contact has moved out of your proximity radius (last seen ~%s meters away).",
                roundedDistance);
    }

    private String roundDistanceForMessage(double distanceMeters) {
        // Round to nearest 50m to avoid revealing precise distance
        long rounded = Math.round(distanceMeters / 50.0) * 50;
        return String.valueOf(Math.max(rounded, 50));
    }
}
