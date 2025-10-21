package com.minishop.notificationservice.event.consumer;

import com.minishop.notificationservice.entity.NotificationChannel;
import com.minishop.notificationservice.entity.ProcessedEvent;
import com.minishop.notificationservice.event.dto.UserRegisteredEvent;
import com.minishop.notificationservice.repository.ProcessedEventRepository;
import com.minishop.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventsConsumer {

    private final NotificationService notificationService;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "user.registered", groupId = "notification-service-group")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received user.registered event for userId: {}", event.getUserId());

        if (isAlreadyProcessed(event.getEventId())) {
            return;
        }

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("userName", event.getFullName() != null ? event.getFullName() : "Thành viên mới");
            params.put("userEmail", event.getEmail());

            notificationService.sendNotification(
                    event.getUserId(),
                    NotificationChannel.EMAIL,
                    event.getEmail(),
                    "WELCOME_EMAIL",
                    params,
                    event.getUserId() != null ? event.getUserId().toString() : null
            );

            markAsProcessed(event.getEventId(), "user.registered");
        } catch (Exception ex) {
            log.error("Error processing user.registered notification for userId: {}: {}", event.getUserId(), ex.getMessage());
        }
    }

    private boolean isAlreadyProcessed(String eventId) {
        if (eventId == null) return false;
        boolean exists = processedEventRepository.existsById(eventId);
        if (exists) {
            log.warn("Event {} already processed, skipping duplicate", eventId);
        }
        return exists;
    }

    private void markAsProcessed(String eventId, String eventType) {
        if (eventId != null) {
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .processedAt(Instant.now())
                    .build());
        }
    }
}
