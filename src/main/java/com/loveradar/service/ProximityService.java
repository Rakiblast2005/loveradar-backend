package com.loveradar.service;

import com.loveradar.dto.alert.AlertResponse;
import com.loveradar.entity.Alert;
import com.loveradar.entity.AlertType;
import com.loveradar.entity.Contact;
import com.loveradar.entity.CoupleSession;
import com.loveradar.entity.Location;
import com.loveradar.entity.User;
import com.loveradar.repository.AlertRepository;
import com.loveradar.repository.ContactRepository;
import com.loveradar.repository.EncounterHistoryRepository;
import com.loveradar.repository.LocationRepository;
import com.loveradar.entity.EncounterHistory;
import com.loveradar.util.HaversineUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Core proximity-detection engine.
 *
 * For an active session, whenever either participant's location is updated,
 * this service checks the trusted contacts of BOTH participants. A contact
 * is "trackable" if it is linked to a registered LoveRadar account (the
 * {@code linkedUser} field on {@link Contact}) and that account has shared
 * a recent location update.
 *
 * For every trackable contact, the distance between the moving participant
 * and the contact's last known location is computed using the Haversine
 * formula and compared against the session's configured radius. Enter/exit
 * transitions generate {@link Alert} records and real-time notifications,
 * while never exposing raw GPS coordinates to either party.
 */
@Service
@RequiredArgsConstructor
public class ProximityService {

    private final ContactRepository contactRepository;
    private final LocationRepository locationRepository;
    private final AlertRepository alertRepository;
    private final EncounterHistoryRepository encounterHistoryRepository;
    private final AlertService alertService;
    private final NotificationService notificationService;

    @Value("${loveradar.proximity.stale-location-seconds:60}")
    private long staleLocationSeconds;

    @Transactional
    public List<AlertResponse> evaluate(CoupleSession session, User movedUser, Location movedLocation) {
        List<AlertResponse> newAlerts = new ArrayList<>();

        Set<Contact> candidateContacts = new HashSet<>();
        candidateContacts.addAll(contactRepository.findByOwnerAndTrustedTrue(session.getCreator()));
        if (session.getPartner() != null) {
            candidateContacts.addAll(contactRepository.findByOwnerAndTrustedTrue(session.getPartner()));
        }

        LocalDateTime staleCutoff = LocalDateTime.now().minusSeconds(staleLocationSeconds);

        for (Contact contact : candidateContacts) {
            User linkedUser = contact.getLinkedUser();
            if (linkedUser == null) {
                continue; // Not a trackable LoveRadar account
            }
            if (linkedUser.getId().equals(movedUser.getId())) {
                continue; // Can't be "near yourself"
            }
            if (!linkedUser.isShareLocation()) {
                continue; // Respect contact's privacy setting
            }

            Optional<Location> contactLocationOpt = locationRepository
                    .findByUserOrderByTimestampDesc(linkedUser)
                    .stream()
                    .findFirst();

            if (contactLocationOpt.isEmpty()) {
                continue;
            }

            Location contactLocation = contactLocationOpt.get();
            if (contactLocation.getTimestamp().isBefore(staleCutoff)) {
                continue; // Contact's location data is too old to trust
            }

            double distance = HaversineUtil.distanceInMeters(
                    movedLocation.getLatitude(), movedLocation.getLongitude(),
                    contactLocation.getLatitude(), contactLocation.getLongitude());

            boolean withinRadius = distance <= session.getRadiusMeters();

            Optional<Alert> lastAlert = alertRepository
                    .findFirstBySessionAndContactOrderByCreatedAtDesc(session, contact);

            boolean wasWithinRadius = lastAlert.isPresent() && lastAlert.get().getAlertType() == AlertType.ENTER;

            if (withinRadius && !wasWithinRadius) {
                Alert alert = alertService.createAlert(session, contact, AlertType.ENTER, distance,
                        movedLocation.getLatitude(), movedLocation.getLongitude());
                incrementEncounter(session.getCreator(), contact);
                if (session.getPartner() != null) {
                    incrementEncounter(session.getPartner(), contact);
                }
                AlertResponse response = alertService.toResponse(alert);
                newAlerts.add(response);
                notificationService.broadcastAlert(session, response);
            } else if (!withinRadius && wasWithinRadius) {
                Alert alert = alertService.createAlert(session, contact, AlertType.EXIT, distance,
                        movedLocation.getLatitude(), movedLocation.getLongitude());
                AlertResponse response = alertService.toResponse(alert);
                newAlerts.add(response);
                notificationService.broadcastAlert(session, response);
            }
        }

        return newAlerts;
    }

    private void incrementEncounter(User user, Contact contact) {
        EncounterHistory history = encounterHistoryRepository.findByUserAndContact(user, contact)
                .orElseGet(() -> EncounterHistory.builder()
                        .user(user)
                        .contact(contact)
                        .encounterCount(0)
                        .build());

        history.setEncounterCount(history.getEncounterCount() + 1);
        history.setLastEncountered(LocalDateTime.now());
        encounterHistoryRepository.save(history);
    }
}
