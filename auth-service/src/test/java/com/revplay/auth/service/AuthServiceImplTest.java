package com.revplay.auth.service;

import com.revplay.auth.dto.AuthResponse;
import com.revplay.auth.dto.LoginRequest;
import com.revplay.auth.dto.RegisterRequest;
import com.revplay.auth.entity.RefreshToken;
import com.revplay.auth.entity.Role;
import com.revplay.auth.entity.User;
import com.revplay.auth.exception.AuthException;
import com.revplay.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_shouldReturnAuthResponse_whenAllValid() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("securePass");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setRole(Role.USER);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPass");

        User savedUser = User.builder()
                .id(1L)
                .email(request.getEmail())
                .password("encodedPass")
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("access-token");

        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-token")
                .userId(savedUser.getId())
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenService.createRefreshToken(savedUser.getId())).thenReturn(refreshToken);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(savedUser);
        verify(refreshTokenService).createRefreshToken(savedUser.getId());
    }

    @Test
    void register_shouldThrowAuthException_whenEmailAlreadyRegistered() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("securePass");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setRole(Role.USER);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("Email already registered");

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_shouldReturnAuthResponse_whenCredentialsValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("securePass");

        User user = User.builder()
                .id(1L)
                .email(request.getEmail())
                .password("encodedPass")
                .firstName("Test")
                .lastName("User")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("access-token");

        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-token")
                .userId(user.getId())
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(refreshToken);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        verify(userRepository).findByEmail(request.getEmail());
    }

    @Test
    void login_shouldThrowAuthException_whenInvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongPass");

        User user = User.builder()
                .id(1L)
                .email(request.getEmail())
                .password("encodedPass")
                .firstName("Test")
                .lastName("User")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void validateToken_shouldReturnTrue_whenJwtServiceReturnsTrue() {
        String token = "some-token";
        when(jwtService.validateToken(token)).thenReturn(true);

        boolean result = authService.validateToken(token);

        assertThat(result).isTrue();
        verify(jwtService).validateToken(token);
    }
}
