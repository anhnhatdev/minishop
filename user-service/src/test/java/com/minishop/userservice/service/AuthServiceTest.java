package com.minishop.userservice.service;

import com.minishop.userservice.dto.request.LoginRequest;
import com.minishop.userservice.dto.request.RegisterRequest;
import com.minishop.userservice.dto.response.AuthResponse;
import com.minishop.userservice.dto.response.UserResponse;
import com.minishop.userservice.entity.Role;
import com.minishop.userservice.entity.User;
import com.minishop.userservice.entity.UserStatus;
import com.minishop.userservice.exception.InvalidCredentialsException;
import com.minishop.userservice.exception.UserAlreadyExistsException;
import com.minishop.userservice.mapper.UserMapper;
import com.minishop.userservice.repository.UserRepository;
import com.minishop.userservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private TokenService tokenService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hashed_password")
                .fullName("Nguyen Van A")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        registerRequest = RegisterRequest.builder()
                .email("user@example.com")
                .password("Passw0rd!")
                .fullName("Nguyen Van A")
                .build();

        loginRequest = LoginRequest.builder()
                .email("user@example.com")
                .password("Passw0rd!")
                .build();
    }

    @Test
    void testRegisterSuccess() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userMapper.toUser(registerRequest)).thenReturn(new User());
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(userMapper.toUserResponse(sampleUser)).thenReturn(
                UserResponse.builder()
                        .id(sampleUser.getId())
                        .email(sampleUser.getEmail())
                        .fullName(sampleUser.getFullName())
                        .role(Role.CUSTOMER)
                        .status(UserStatus.ACTIVE)
                        .build()
        );

        UserResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("user@example.com", response.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateEmailThrowsException() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Passw0rd!", "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("mock_access_token");
        when(tokenService.createRefreshToken(any(), any())).thenReturn("mock_refresh_token");
        when(jwtTokenProvider.getAccessTokenExpirationInMs()).thenReturn(900000L);

        AuthResponse response = authService.login(loginRequest, "Mozilla/5.0");

        assertNotNull(response);
        assertEquals("mock_access_token", response.getAccessToken());
        assertEquals("mock_refresh_token", response.getRefreshToken());
        assertEquals(900L, response.getExpiresIn());
    }

    @Test
    void testLoginWrongPasswordThrowsException() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Passw0rd!", "hashed_password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest, "Mozilla/5.0"));
    }
}
