package com.loveradar.service;

import com.loveradar.dto.alert.AlertResponse;
import com.loveradar.entity.*;
import com.loveradar.repository.AlertRepository;
import com.loveradar.repository.ContactRepository;
import com.loveradar.repository.EncounterHistoryRepository;
import com.loveradar.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProximityServiceTest {

    @Mock
    private ContactRepository contactRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private EncounterHistoryRepository encounterHistoryRepository;
    @Mock
    private AlertService alertService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProximityService proximityService;

    private User creator;
    private User partner;
    private User trackedContactUser;
    private CoupleSession session;
    private Contact trackedContact;

    @BeforeEach
    void setUp() {
        creator = User.builder().id(UUID.randomUUID()).name("Alex").shareLocation(true).build();
        partner = User.builder().id(UUID.randomUUID()).name("Sam").shareLocation(true).build();
        trackedContactUser = User.builder().id(UUID.randomUUID()).name("Jordan").shareLocation(true).build();

        session = CoupleSession.builder()
                .id(UUID.randomUUID())
                .creator(creator)
                .partner(partner)
                .radiusMeters(250)
                .active(true)
                .build();

        trackedContact = Contact.builder()
                .id(UUID.randomUUID())
                .owner(creator)
                .contactName("Jordan")
                .phoneNumber("+10000000000")
                .linkedUser(trackedContactUser)
                .trusted(true)
                .build();

        org.springframework.test.util.ReflectionTestUtils.setField(proximityService, "staleLocationSeconds", 60L);
    }

    @Test
    void evaluate_createsEnterAlert_whenContactComesWithinRadius() {
        Location movedLocation = Location.builder()
                .user(creator).session(session)
                .latitude(13.0827).longitude(80.2707)
                .timestamp(LocalDateTime.now())
                .build();

        Location contactLocation = Location.builder()
                .user(trackedContactUser).session(null)
                .latitude(13.0830).longitude(80.2710)
                .timestamp(LocalDateTime.now())
                .build();

        when(contactRepository.findByOwnerAndTrustedTrue(creator)).thenReturn(List.of(trackedContact));
        when(contactRepository.findByOwnerAndTrustedTrue(partner)).thenReturn(List.of());
        when(locationRepository.findByUserOrderByTimestampDesc(trackedContactUser))
                .thenReturn(List.of(contactLocation));
        when(alertRepository.findFirstBySessionAndContactOrderByCreatedAtDesc(session, trackedContact))
                .thenReturn(Optional.empty());
        when(encounterHistoryRepository.findByUserAndContact(any(), any())).thenReturn(Optional.empty());

        Alert savedAlert = Alert.builder()
                .id(UUID.randomUUID())
                .session(session)
                .contact(trackedContact)
                .contactName("Jordan")
                .alertType(AlertType.ENTER)
                .alertLevel(AlertLevel.WARNING)
                .message("Alert: A contact from your trusted list is within 50 meters.")
                .distance(40.0)
                .createdAt(LocalDateTime.now())
                .build();

        when(alertService.createAlert(eq(session), eq(trackedContact), eq(AlertType.ENTER), any(Double.class),
                any(), any())).thenReturn(savedAlert);
        when(alertService.toResponse(savedAlert)).thenReturn(AlertResponse.builder()
                .id(savedAlert.getId().toString())
                .contactName("Jordan")
                .alertType("ENTER")
                .build());

        List<AlertResponse> alerts = proximityService.evaluate(session, creator, movedLocation);

        assertEquals(1, alerts.size());
        assertEquals("ENTER", alerts.get(0).getAlertType());
        verify(alertService).createAlert(eq(session), eq(trackedContact), eq(AlertType.ENTER), any(Double.class), any(), any());
        verify(notificationService).broadcastAlert(eq(session), any());
        verify(encounterHistoryRepository, atLeastOnce()).save(any(EncounterHistory.class));
    }

    @Test
    void evaluate_skipsContact_whenLastLocationIsStale() {
        Location movedLocation = Location.builder()
                .user(creator).session(session)
                .latitude(13.0827).longitude(80.2707)
                .timestamp(LocalDateTime.now())
                .build();

        Location staleLocation = Location.builder()
                .user(trackedContactUser).session(null)
                .latitude(13.0830).longitude(80.2710)
                .timestamp(LocalDateTime.now().minusMinutes(10))
                .build();

        when(contactRepository.findByOwnerAndTrustedTrue(creator)).thenReturn(List.of(trackedContact));
        when(contactRepository.findByOwnerAndTrustedTrue(partner)).thenReturn(List.of());
        when(locationRepository.findByUserOrderByTimestampDesc(trackedContactUser))
                .thenReturn(List.of(staleLocation));

        List<AlertResponse> alerts = proximityService.evaluate(session, creator, movedLocation);

        assertTrue(alerts.isEmpty());
        verify(alertService, never()).createAlert(any(), any(), any(), any(Double.class), any(), any());
        verify(notificationService, never()).broadcastAlert(any(), any());
    }

    @Test
    void evaluate_createsExitAlert_whenContactLeavesRadius() {
        Location movedLocation = Location.builder()
                .user(creator).session(session)
                .latitude(13.0827).longitude(80.2707)
                .timestamp(LocalDateTime.now())
                .build();

        Location farLocation = Location.builder()
                .user(trackedContactUser).session(null)
                .latitude(13.2000).longitude(80.4000)
                .timestamp(LocalDateTime.now())
                .build();

        Alert previousEnterAlert = Alert.builder()
                .id(UUID.randomUUID())
                .session(session)
                .contact(trackedContact)
                .alertType(AlertType.ENTER)
                .alertLevel(AlertLevel.WARNING)
                .distance(50.0)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(contactRepository.findByOwnerAndTrustedTrue(creator)).thenReturn(List.of(trackedContact));
        when(contactRepository.findByOwnerAndTrustedTrue(partner)).thenReturn(List.of());
        when(locationRepository.findByUserOrderByTimestampDesc(trackedContactUser))
                .thenReturn(List.of(farLocation));
        when(alertRepository.findFirstBySessionAndContactOrderByCreatedAtDesc(session, trackedContact))
                .thenReturn(Optional.of(previousEnterAlert));

        Alert exitAlert = Alert.builder()
                .id(UUID.randomUUID())
                .session(session)
                .contact(trackedContact)
                .contactName("Jordan")
                .alertType(AlertType.EXIT)
                .alertLevel(AlertLevel.INFO)
                .distance(15000.0)
                .createdAt(LocalDateTime.now())
                .build();

        when(alertService.createAlert(eq(session), eq(trackedContact), eq(AlertType.EXIT), any(Double.class),
                any(), any())).thenReturn(exitAlert);
        when(alertService.toResponse(exitAlert)).thenReturn(AlertResponse.builder()
                .id(exitAlert.getId().toString())
                .contactName("Jordan")
                .alertType("EXIT")
                .build());

        List<AlertResponse> alerts = proximityService.evaluate(session, creator, movedLocation);

        assertEquals(1, alerts.size());
        assertEquals("EXIT", alerts.get(0).getAlertType());
        verify(encounterHistoryRepository, never()).save(any());
    }

    @Test
    void evaluate_skipsContact_whenLinkedToTheMovingUserThemselves() {
        Contact selfLinkedContact = Contact.builder()
                .id(UUID.randomUUID())
                .owner(creator)
                .contactName("Me")
                .phoneNumber("+1")
                .linkedUser(creator)
                .trusted(true)
                .build();

        Location movedLocation = Location.builder()
                .user(creator).session(session)
                .latitude(13.0827).longitude(80.2707)
                .timestamp(LocalDateTime.now())
                .build();

        when(contactRepository.findByOwnerAndTrustedTrue(creator)).thenReturn(List.of(selfLinkedContact));
        when(contactRepository.findByOwnerAndTrustedTrue(partner)).thenReturn(List.of());

        List<AlertResponse> alerts = proximityService.evaluate(session, creator, movedLocation);

        assertTrue(alerts.isEmpty());
        verify(locationRepository, never()).findByUserOrderByTimestampDesc(any());
    }
}
