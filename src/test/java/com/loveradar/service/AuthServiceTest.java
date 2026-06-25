package com.loveradar.service;

import com.loveradar.dto.auth.AuthResponse;
import com.loveradar.dto.auth.LoginRequest;
import com.loveradar.dto.auth.RegisterRequest;
import com.loveradar.entity.RefreshToken;
import com.loveradar.entity.Role;
import com.loveradar.entity.User;
import com.loveradar.exception.ConflictException;
import com.loveradar.exception.UnauthorizedException;
import com.loveradar.repository.RefreshTokenRepository;
import com.loveradar.repository.UserRepository;
import com.loveradar.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(UUID.randomUUID())
                .name("Alex Smith")
                .email("alex@example.com")
                .password("hashed-password")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void register_createsNewUser_whenEmailNotTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alex Smith");
        request.setEmail("alex@example.com");
        request.setPassword("StrongPassword123");

        when(userRepository.existsByEmail("alex@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPassword123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(jwtUtil.generateAccessToken(any(), any())).thenReturn("access-token");
        when(jwtUtil.getAccessExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(request);

        assertEquals("access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("alex@example.com", response.getUser().getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_throwsConflict_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alex Smith");
        request.setEmail("alex@example.com");
        request.setPassword("StrongPassword123");

        when(userRepository.existsByEmail("alex@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_succeeds_withCorrectCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("alex@example.com");
        request.setPassword("StrongPassword123");

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("StrongPassword123", "hashed-password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(any(), any())).thenReturn("access-token");
        when(jwtUtil.getAccessExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    void login_throwsUnauthorized_withIncorrectPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("alex@example.com");
        request.setPassword("WrongPassword");

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("WrongPassword", "hashed-password")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void login_throwsUnauthorized_whenUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@example.com");
        request.setPassword("anything");

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void refresh_throwsUnauthorized_whenTokenExpired() {
        RefreshToken expired = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(existingUser)
                .token("expired-token")
                .expiryDate(LocalDateTime.now().minusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThrows(UnauthorizedException.class, () -> authService.refresh("expired-token"));
    }
}
