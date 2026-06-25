package com.loveradar.service;

import com.loveradar.dto.alert.AlertResponse;
import com.loveradar.entity.CoupleSession;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Publishes real-time events to subscribed session participants over STOMP.
 * Topics:
 *   /topic/sessions/{sessionId}/alerts  - new proximity alerts
 *   /topic/sessions/{sessionId}/status  - partner status updates
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastAlert(CoupleSession session, AlertResponse alert) {
        messagingTemplate.convertAndSend("/topic/sessions/" + session.getId() + "/alerts", alert);
    }

    public void broadcastPartnerStatus(CoupleSession session, String status) {
        messagingTemplate.convertAndSend("/topic/sessions/" + session.getId() + "/status",
                Map.of("partnerStatus", status));
    }
}
