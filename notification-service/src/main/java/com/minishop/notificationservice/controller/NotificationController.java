package com.minishop.notificationservice.controller;

import com.minishop.notificationservice.dto.response.NotificationLogResponse;
import com.minishop.notificationservice.dto.response.NotificationTemplateResponse;
import com.minishop.notificationservice.dto.response.ResendNotificationResponse;
import com.minishop.notificationservice.entity.NotificationStatus;
import com.minishop.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "Endpoints for viewing notification audit logs, templates, and manual resends")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Query notification delivery audit logs with filters and pagination")
    public ResponseEntity<Page<NotificationLogResponse>> getNotificationLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) String referenceId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<NotificationLogResponse> logs = notificationService.getLogs(userId, status, referenceId, pageable);
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/{logId}/resend")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Manually trigger redelivery of a failed notification")
    public ResponseEntity<ResendNotificationResponse> resendNotification(@PathVariable UUID logId) {
        ResendNotificationResponse response = notificationService.resendNotification(logId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: List all notification templates and placeholders")
    public ResponseEntity<List<NotificationTemplateResponse>> getTemplates() {
        List<NotificationTemplateResponse> templates = notificationService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }
}
