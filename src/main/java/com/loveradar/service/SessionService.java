package com.loveradar.service;

import com.loveradar.dto.session.CreateSessionRequest;
import com.loveradar.dto.session.SessionResponse;
import com.loveradar.dto.session.UpdateRadiusRequest;
import com.loveradar.entity.CoupleSession;
import com.loveradar.entity.User;
import com.loveradar.exception.BadRequestException;
import com.loveradar.exception.ConflictException;
import com.loveradar.exception.ResourceNotFoundException;
import com.loveradar.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final UserService userService;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public SessionResponse createSession(CreateSessionRequest request) {
        User creator = userService.getCurrentUser();

        sessionRepository.findActiveSessionForUser(creator).ifPresent(existing -> {
            throw new ConflictException("You already have an active session. End it before creating a new one.");
        });

        int radius = UserService.validateRadius(request.getRadiusMeters());

        CoupleSession session = CoupleSession.builder()
                .sessionCode(generateUniqueCode())
                .creator(creator)
                .radiusMeters(radius)
                .active(true)
                .startTime(LocalDateTime.now())
                .build();

        session = sessionRepository.save(session);
        return toResponse(session);
    }

    @Transactional
    public SessionResponse joinSession(String sessionCodeRaw) {
        User partner = userService.getCurrentUser();
        String sessionCode = sessionCodeRaw.trim().toUpperCase();

        CoupleSession session = sessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new ResourceNotFoundException("No session found with that code"));

        if (!session.isActive()) {
            throw new BadRequestException("This session has already ended");
        }
        if (session.getCreator().getId().equals(partner.getId())) {
            throw new BadRequestException("You cannot join your own session");
        }
        if (session.getPartner() != null && !session.getPartner().getId().equals(partner.getId())) {
            throw new ConflictException("This session already has two participants");
        }

        final CoupleSession currentSession = session;

sessionRepository.findActiveSessionForUser(partner).ifPresent(existing -> {
    if (!existing.getId().equals(currentSession.getId())) {
        throw new ConflictException(
                "You already have an active session. End it before joining another.");
    }
});

currentSession.setPartner(partner);

CoupleSession savedSession = sessionRepository.save(currentSession);

return toResponse(savedSession);
    }

    @Transactional
    public SessionResponse endSession() {
        User user = userService.getCurrentUser();
        CoupleSession session = sessionRepository.findActiveSessionForUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("No active session found"));

        session.setActive(false);
        session.setEndTime(LocalDateTime.now());
        session = sessionRepository.save(session);
        return toResponse(session);
    }

    @Transactional
    public SessionResponse updateRadius(UpdateRadiusRequest request) {
        User user = userService.getCurrentUser();
        CoupleSession session = sessionRepository.findActiveSessionForUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("No active session found"));

        session.setRadiusMeters(UserService.validateRadius(request.getRadiusMeters()));
        session = sessionRepository.save(session);
        return toResponse(session);
    }

    @Transactional
    public SessionResponse setEmergencyHidden(boolean hidden) {
        User user = userService.getCurrentUser();
        CoupleSession session = sessionRepository.findActiveSessionForUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("No active session found"));

        session.setEmergencyHidden(hidden);
        session = sessionRepository.save(session);
        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public Optional<SessionResponse> getActiveSession() {
        User user = userService.getCurrentUser();
        return sessionRepository.findActiveSessionForUser(user).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<CoupleSession> getActiveSessionEntity(User user) {
        return sessionRepository.findActiveSessionForUser(user);
    }

    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
            attempts++;
        } while (sessionRepository.findBySessionCode(code).isPresent() && attempts < 10);
        return code;
    }

    /**
     * ISSUE #6 FIX: toResponse() was package-private (no modifier) which
     * prevented DashboardService (same package) from calling it — actually
     * package-private IS accessible within the same package. The real problem
     * was that DashboardService called sessionService.toResponse() on an
     * injected bean, and package-private methods ARE visible to callers in
     * the same package when accessed via a reference (not reflection).
     * However, making it public is cleaner and avoids confusion.
     */
    public SessionResponse toResponse(CoupleSession session) {
        Long durationSeconds = null;
        if (session.getStartTime() != null) {
            LocalDateTime end = session.getEndTime() != null ? session.getEndTime() : LocalDateTime.now();
            durationSeconds = Duration.between(session.getStartTime(), end).getSeconds();
        }

        return SessionResponse.builder()
                .id(session.getId().toString())
                .sessionCode(session.getSessionCode())
                .creatorName(session.getCreator().getName())
                .partnerName(session.getPartner() != null ? session.getPartner().getName() : null)
                .partnerJoined(session.getPartner() != null)
                .radiusMeters(session.getRadiusMeters())
                .startTime(session.getStartTime() != null ? session.getStartTime().format(ISO) : null)
                .endTime(session.getEndTime() != null ? session.getEndTime().format(ISO) : null)
                .active(session.isActive())
                .emergencyHidden(session.isEmergencyHidden())
                .durationSeconds(durationSeconds)
                .build();
    }
}
