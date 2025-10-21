package com.minishop.notificationservice.dto.response;

import com.minishop.notificationservice.entity.NotificationChannel;
import com.minishop.notificationservice.entity.NotificationStatus;
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
public class NotificationLogResponse {

    private UUID id;
    private UUID userId;
    private NotificationChannel channel;
    private String recipient;
    private String templateCode;
    private String renderedSubject;
    private String renderedContent;
    private NotificationStatus status;
    private String errorMessage;
    private Integer retryCount;
    private String referenceId;
    private Instant createdAt;
    private Instant sentAt;
}
