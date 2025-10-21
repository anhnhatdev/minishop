package com.minishop.notificationservice.dto.response;

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
public class ResendNotificationResponse {

    private UUID logId;
    private NotificationStatus status;
    private Integer retryCount;
    private String message;
    private Instant sentAt;
}
