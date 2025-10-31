package com.minishop.userservice.service;

import com.minishop.userservice.dto.request.LoginRequest;
import com.minishop.userservice.dto.request.RefreshTokenRequest;
import com.minishop.userservice.dto.request.RegisterRequest;
import com.minishop.userservice.dto.response.AuthResponse;
import com.minishop.userservice.dto.response.UserResponse;
import com.minishop.userservice.entity.RefreshToken;
import com.minishop.userservice.entity.Role;
import com.minishop.userservice.entity.User;
import com.minishop.userservice.entity.UserStatus;
import com.minishop.userservice.exception.AccountLockedException;
import com.minishop.userservice.exception.InvalidCredentialsException;
import com.minishop.userservice.exception.UserAlreadyExistsException;
import com.minishop.userservice.mapper.UserMapper;
import com.minishop.userservice.repository.UserRepository;
import com.minishop.userservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final UserMapper userMapper;
    private final com.minishop.userservice.event.producer.UserEventProducer userEventProducer;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        User user = userMapper.toUser(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        // Publish user.registered event for welcome email notification
        try {
            com.minishop.userservice.event.dto.UserRegisteredEvent event =
                    com.minishop.userservice.event.dto.UserRegisteredEvent.builder()
                            .eventId(java.util.UUID.randomUUID().toString())
                            .eventType("user.registered")
                            .userId(savedUser.getId())
                            .email(savedUser.getEmail())
                            .fullName(savedUser.getFullName())
                            .timestamp(java.time.Instant.now())
                            .build();
            userEventProducer.publishUserRegistered(event);
        } catch (Exception ex) {
            // Event publication failure must not block registration transaction
        }

        return userMapper.toUserResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String deviceInfo) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AccountLockedException("Account has been locked. Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = tokenService.createRefreshToken(user, deviceInfo);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationInMs() / 1000)
                .build();
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, String deviceInfo) {
        RefreshToken oldRefreshToken = tokenService.verifyAndGetRefreshToken(request.getRefreshToken());
        User user = oldRefreshToken.getUser();

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AccountLockedException("Account has been locked. Cannot refresh token.");
        }

        // Token Rotation: revoke old refresh token and issue new one
        tokenService.revokeRefreshToken(request.getRefreshToken());

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String newRefreshToken = tokenService.createRefreshToken(user, deviceInfo);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationInMs() / 1000)
                .build();
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        tokenService.revokeRefreshToken(request.getRefreshToken());
    }
}
