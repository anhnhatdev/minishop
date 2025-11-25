package com.minishop.notificationservice.service;

import com.minishop.notificationservice.dto.response.NotificationLogResponse;
import com.minishop.notificationservice.dto.response.NotificationTemplateResponse;
import com.minishop.notificationservice.dto.response.ResendNotificationResponse;
import com.minishop.notificationservice.entity.NotificationChannel;
import com.minishop.notificationservice.entity.NotificationLog;
import com.minishop.notificationservice.entity.NotificationStatus;
import com.minishop.notificationservice.entity.NotificationTemplate;
import com.minishop.notificationservice.exception.TemplateNotFoundException;
import com.minishop.notificationservice.mapper.NotificationMapper;
import com.minishop.notificationservice.repository.NotificationLogRepository;
import com.minishop.notificationservice.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;
    private final TemplateRenderService renderService;
    private final EmailSenderService emailSenderService;
    private final SmsSenderService smsSenderService;
    private final NotificationMapper notificationMapper;

    @Transactional
    public NotificationLog sendNotification(
            UUID userId,
            NotificationChannel channel,
            String recipient,
            String templateCode,
            Map<String, Object> params,
            String referenceId
    ) {
        log.info("Initiating notification: template='{}', channel='{}', recipient='{}', ref='{}'",
                templateCode, channel, recipient, referenceId);

        NotificationTemplate template = templateRepository.findByCodeAndChannel(templateCode, channel)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found for code: " + templateCode + " and channel: " + channel));

        String renderedSubject = (template.getSubject() != null)
                ? renderService.render(template.getSubject(), params)
                : null;
        String renderedContent = renderService.render(template.getBodyTemplate(), params);

        NotificationLog logEntry = NotificationLog.builder()
                .userId(userId)
                .channel(channel)
                .recipient(recipient)
                .templateCode(templateCode)
                .renderedSubject(renderedSubject)
                .renderedContent(renderedContent)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .referenceId(referenceId)
                .createdAt(Instant.now())
                .build();

        logEntry = logRepository.save(logEntry);

        // Execute delivery
        deliver(logEntry);

        return logRepository.save(logEntry);
    }

    @Transactional
    public ResendNotificationResponse resendNotification(UUID logId) {
        NotificationLog logEntry = logRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Notification log not found for ID: " + logId));

        logEntry.setRetryCount(logEntry.getRetryCount() + 1);
        deliver(logEntry);

        NotificationLog saved = logRepository.save(logEntry);

        return ResendNotificationResponse.builder()
                .logId(saved.getId())
                .status(saved.getStatus())
                .retryCount(saved.getRetryCount())
                .message(saved.getStatus() == NotificationStatus.SENT ? "Resent successfully" : "Resend failed: " + saved.getErrorMessage())
                .sentAt(saved.getSentAt())
                .build();
    }

    private void deliver(NotificationLog logEntry) {
        try {
            if (logEntry.getChannel() == NotificationChannel.EMAIL) {
                emailSenderService.sendHtmlEmail(
                        logEntry.getRecipient(),
                        logEntry.getRenderedSubject() != null ? logEntry.getRenderedSubject() : "MiniShop Notification",
                        logEntry.getRenderedContent()
                );
            } else if (logEntry.getChannel() == NotificationChannel.SMS) {
                smsSenderService.sendSms(logEntry.getRecipient(), logEntry.getRenderedContent());
            }

            logEntry.setStatus(NotificationStatus.SENT);
            logEntry.setSentAt(Instant.now());
            logEntry.setErrorMessage(null);
            log.info("Notification successfully sent to {}", logEntry.getRecipient());
        } catch (Exception ex) {
            logEntry.setStatus(NotificationStatus.FAILED);
            logEntry.setErrorMessage(ex.getMessage() != null ? ex.getMessage() : "Unknown delivery error");
            log.error("Failed to send notification to {}: {}", logEntry.getRecipient(), ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationLogResponse> getLogs(UUID userId, NotificationStatus status, String referenceId, Pageable pageable) {
        return logRepository.findWithFilters(userId, status, referenceId, pageable)
                .map(notificationMapper::toNotificationLogResponse);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getAllTemplates() {
        return notificationMapper.toNotificationTemplateResponseList(templateRepository.findAll());
    }

    @Transactional
    public void retryFailedNotifications() {
        List<NotificationLog> failedLogs = logRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3);
        if (!failedLogs.isEmpty()) {
            Instant now = Instant.now();
            for (NotificationLog logEntry : failedLogs) {
                // Exponential backoff: retry 1 after 60s, retry 2 after 300s (5m), retry 3 after 900s (15m)
                long minIntervalSeconds = (long) Math.pow(5, logEntry.getRetryCount()) * 12; // 12s, 60s, 300s
                if (logEntry.getCreatedAt() != null && logEntry.getCreatedAt().plusSeconds(minIntervalSeconds).isBefore(now)) {
                    log.info("Retrying failed notification ID: {} (attempt {}/3)", logEntry.getId(), logEntry.getRetryCount() + 1);
                    logEntry.setRetryCount(logEntry.getRetryCount() + 1);
                    deliver(logEntry);
                    logRepository.save(logEntry);
                }
            }
        }
    }
}
