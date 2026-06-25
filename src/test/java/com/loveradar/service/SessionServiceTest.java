package com.loveradar.service;

import com.loveradar.dto.session.CreateSessionRequest;
import com.loveradar.dto.session.SessionResponse;
import com.loveradar.entity.CoupleSession;
import com.loveradar.entity.Role;
import com.loveradar.entity.User;
import com.loveradar.exception.BadRequestException;
import com.loveradar.exception.ConflictException;
import com.loveradar.exception.ResourceNotFoundException;
import com.loveradar.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService")
class SessionServiceTest {

    @Mock SessionRepository sessionRepository;
    @Mock UserService userService;

    @InjectMocks SessionService sessionService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(UUID.randomUUID()).name("Alice").email("alice@example.com")
                .password("x").role(Role.USER).build();
        bob = User.builder().id(UUID.randomUUID()).name("Bob").email("bob@example.com")
                .password("x").role(Role.USER).build();
    }

    @Nested
    @DisplayName("createSession()")
    class Create {

        @Test
        @DisplayName("creates session with valid radius")
        void happyPath() {
            when(userService.getCurrentUser()).thenReturn(alice);
            when(sessionRepository.findActiveSessionForUser(alice)).thenReturn(Optional.empty());
            when(sessionRepository.findBySessionCode(anyString())).thenReturn(Optional.empty());

            CoupleSession saved = CoupleSession.builder()
                    .id(UUID.randomUUID()).sessionCode("ABC123")
                    .creator(alice).radiusMeters(250).active(true)
                    .startTime(LocalDateTime.now()).build();
            when(sessionRepository.save(any())).thenReturn(saved);

            CreateSessionRequest req = new CreateSessionRequest();
            req.setRadiusMeters(250);

            SessionResponse response = sessionService.createSession(req);

            assertThat(response.getSessionCode()).isEqualTo("ABC123");
            assertThat(response.isActive()).isTrue();
        }

        @Test
        @DisplayName("throws ConflictException when user already has active session")
        void alreadyActive() {
            CoupleSession existing = CoupleSession.builder()
                    .id(UUID.randomUUID()).sessionCode("EXIST1")
                    .creator(alice).active(true).build();
            when(userService.getCurrentUser()).thenReturn(alice);
            when(sessionRepository.findActiveSessionForUser(alice)).thenReturn(Optional.of(existing));

            CreateSessionRequest req = new CreateSessionRequest();
            req.setRadiusMeters(500);

            assertThatThrownBy(() -> sessionService.createSession(req))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("throws BadRequestException for invalid radius")
        void invalidRadius() {
            when(userService.getCurrentUser()).thenReturn(alice);
            when(sessionRepository.findActiveSessionForUser(alice)).thenReturn(Optional.empty());

            CreateSessionRequest req = new CreateSessionRequest();
            req.setRadiusMeters(999); // invalid

            assertThatThrownBy(() -> sessionService.createSession(req))
                    .isInstanceOf(com.loveradar.exception.BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("joinSession()")
    class Join {

        @Test
        @DisplayName("bob can join alice's session")
        void happyPath() {
            CoupleSession session = CoupleSession.builder()
                    .id(UUID.randomUUID()).sessionCode("ABC123")
                    .creator(alice).active(true).radiusMeters(250)
                    .startTime(LocalDateTime.now()).build();

            when(userService.getCurrentUser()).thenReturn(bob);
            when(sessionRepository.findBySessionCode("ABC123")).thenReturn(Optional.of(session));
            when(sessionRepository.findActiveSessionForUser(bob)).thenReturn(Optional.empty());

            CoupleSession updated = CoupleSession.builder()
                    .id(session.getId()).sessionCode("ABC123")
                    .creator(alice).partner(bob).active(true)
                    .radiusMeters(250).startTime(LocalDateTime.now()).build();
            when(sessionRepository.save(any())).thenReturn(updated);

            SessionResponse response = sessionService.joinSession("abc123"); // lowercase OK

            assertThat(response.getPartnerName()).isEqualTo("Bob");
        }

        @Test
        @DisplayName("throws BadRequestException when creator tries to join own session")
        void creatorJoinsOwn() {
            CoupleSession session = CoupleSession.builder()
                    .id(UUID.randomUUID()).sessionCode("ABC123")
                    .creator(alice).active(true).build();

            when(userService.getCurrentUser()).thenReturn(alice);
            when(sessionRepository.findBySessionCode("ABC123")).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> sessionService.joinSession("ABC123"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown code")
        void unknownCode() {
            when(userService.getCurrentUser()).thenReturn(bob);
            when(sessionRepository.findBySessionCode("XXXXXX")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sessionService.joinSession("XXXXXX"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("endSession()")
    class End {

        @Test
        @DisplayName("marks session as inactive")
        void happyPath() {
            CoupleSession session = CoupleSession.builder()
                    .id(UUID.randomUUID()).sessionCode("ABC123")
                    .creator(alice).active(true)
                    .startTime(LocalDateTime.now().minusMinutes(10)).build();

            when(userService.getCurrentUser()).thenReturn(alice);
            when(sessionRepository.findActiveSessionForUser(alice)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SessionResponse response = sessionService.endSession();

            assertThat(response.isActive()).isFalse();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when no active session")
        void noneActive() {
            when(userService.getCurrentUser()).thenReturn(alice);
            when(sessionRepository.findActiveSessionForUser(alice)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sessionService.endSession())
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
