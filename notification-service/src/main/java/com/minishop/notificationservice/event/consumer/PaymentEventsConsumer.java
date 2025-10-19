package com.minishop.notificationservice.event.consumer;

import com.minishop.notificationservice.entity.NotificationChannel;
import com.minishop.notificationservice.entity.ProcessedEvent;
import com.minishop.notificationservice.event.dto.PaymentFailedEvent;
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
public class PaymentEventsConsumer {

    private final NotificationService notificationService;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "payment.failed", groupId = "notification-service-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Received payment.failed event for orderId: {}", event.getOrderId());

        if (isAlreadyProcessed(event.getEventId())) {
            return;
        }

        try {
            String recipient = (event.getCustomerEmail() != null && !event.getCustomerEmail().isBlank())
                    ? event.getCustomerEmail()
                    : "customer@example.com";

            Map<String, Object> params = new HashMap<>();
            params.put("orderCode", event.getOrderCode() != null ? event.getOrderCode() : event.getOrderId().toString());
            params.put("customerName", event.getCustomerName() != null ? event.getCustomerName() : "Quý khách");
            params.put("totalAmount", event.getTotalAmount() != null ? event.getTotalAmount().toString() : "0");
            params.put("failureReason", event.getReason() != null ? event.getReason() : "Giao dịch không hoàn tất");

            notificationService.sendNotification(
                    event.getUserId(),
                    NotificationChannel.EMAIL,
                    recipient,
                    "PAYMENT_FAILED",
                    params,
                    event.getOrderId() != null ? event.getOrderId().toString() : null
            );

            markAsProcessed(event.getEventId(), "payment.failed");
        } catch (Exception ex) {
            log.error("Error processing payment.failed notification for orderId: {}: {}", event.getOrderId(), ex.getMessage());
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
