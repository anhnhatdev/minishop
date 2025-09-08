package com.minishop.userservice.dto.response;

import com.minishop.userservice.entity.Role;
import com.minishop.userservice.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private Role role;
    private UserStatus status;
    private Boolean emailVerified;
    private Instant createdAt;
}
