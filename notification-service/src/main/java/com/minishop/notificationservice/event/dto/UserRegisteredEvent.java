package com.minishop.notificationservice.event.dto;

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
public class UserRegisteredEvent {

    private String eventId;
    private String eventType;
    private UUID userId;
    private String email;
    private String fullName;
    private Instant timestamp;
}
