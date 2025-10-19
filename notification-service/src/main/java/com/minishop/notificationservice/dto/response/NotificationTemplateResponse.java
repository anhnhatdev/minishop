package com.minishop.notificationservice.dto.response;

import com.minishop.notificationservice.entity.NotificationChannel;
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
public class NotificationTemplateResponse {

    private UUID id;
    private String code;
    private NotificationChannel channel;
    private String subject;
    private String bodyTemplate;
    private Instant createdAt;
    private Instant updatedAt;
}
