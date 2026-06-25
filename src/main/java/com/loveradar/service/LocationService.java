package com.loveradar.service;

import com.loveradar.dto.location.LocationUpdateRequest;
import com.loveradar.dto.location.LocationUpdateResponse;
import com.loveradar.entity.CoupleSession;
import com.loveradar.entity.Location;
import com.loveradar.entity.User;
import com.loveradar.exception.BadRequestException;
import com.loveradar.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final UserService userService;
    private final SessionService sessionService;
    private final ProximityService proximityService;
    private final NotificationService notificationService;

    @Value("${loveradar.proximity.stale-location-seconds:60}")
    private long staleLocationSeconds;

    @Transactional
    public LocationUpdateResponse updateLocation(LocationUpdateRequest request) {
        User user = userService.getCurrentUser();

        if (!user.isShareLocation()) {
            throw new BadRequestException(
                    "Location sharing is disabled in your privacy settings. Enable it to use live tracking.");
        }

        CoupleSession session = sessionService.getActiveSessionEntity(user)
                .orElseThrow(() -> new BadRequestException("You do not have an active session"));

        Location location = Location.builder()
                .user(user)
                .session(session)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .timestamp(LocalDateTime.now())
                .build();

        location = locationRepository.save(location);

        var newAlerts = proximityService.evaluate(session, user, location);

        String partnerStatus = resolvePartnerStatus(session, user);
        notificationService.broadcastPartnerStatus(session, partnerStatus);

        return LocationUpdateResponse.builder()
                .status("LOCATION_RECORDED")
                .partnerStatus(partnerStatus)
                .newAlerts(newAlerts)
                .build();
    }

    /**
     * Resolves the partner's connectivity status without ever exposing
     * their coordinates: ONLINE (recent update), AWAY (stale), or
     * NOT_JOINED (no partner in session yet).
     */
    public String resolvePartnerStatus(CoupleSession session, User self) {
        User partner = session.getCreator().getId().equals(self.getId())
                ? session.getPartner()
                : session.getCreator();

        if (partner == null) {
            return "NOT_JOINED";
        }

        Optional<Location> latest = locationRepository
                .findByUserOrderByTimestampDesc(partner)
                .stream()
                .findFirst();

        if (latest.isEmpty()) {
            return "AWAY";
        }

        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(staleLocationSeconds);
        return latest.get().getTimestamp().isAfter(cutoff) ? "ONLINE" : "AWAY";
    }
}
