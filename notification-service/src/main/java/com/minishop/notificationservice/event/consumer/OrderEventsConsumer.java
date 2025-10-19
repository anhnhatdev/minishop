package com.minishop.notificationservice.event.consumer;

import com.minishop.notificationservice.entity.NotificationChannel;
import com.minishop.notificationservice.entity.ProcessedEvent;
import com.minishop.notificationservice.event.dto.OrderCancelledEvent;
import com.minishop.notificationservice.event.dto.OrderConfirmedEvent;
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
public class OrderEventsConsumer {

    private final NotificationService notificationService;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "order.confirmed", groupId = "notification-service-group")
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Received order.confirmed event for orderId: {}", event.getOrderId());

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
            params.put("shippingAddress", event.getShippingAddress() != null ? event.getShippingAddress() : "Địa chỉ đã đăng ký");

            notificationService.sendNotification(
                    event.getUserId(),
                    NotificationChannel.EMAIL,
                    recipient,
                    "ORDER_CONFIRMED",
                    params,
                    event.getOrderId() != null ? event.getOrderId().toString() : null
            );

            markAsProcessed(event.getEventId(), "order.confirmed");
        } catch (Exception ex) {
            log.error("Error processing order.confirmed notification for orderId: {}: {}", event.getOrderId(), ex.getMessage());
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "notification-service-group")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received order.cancelled event for orderId: {}", event.getOrderId());

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
            params.put("cancelReason", event.getReason() != null ? event.getReason() : "Hệ thống tự động hủy theo yêu cầu hoặc quá hạn");

            notificationService.sendNotification(
                    event.getUserId(),
                    NotificationChannel.EMAIL,
                    recipient,
                    "ORDER_CANCELLED",
                    params,
                    event.getOrderId() != null ? event.getOrderId().toString() : null
            );

            markAsProcessed(event.getEventId(), "order.cancelled");
        } catch (Exception ex) {
            log.error("Error processing order.cancelled notification for orderId: {}: {}", event.getOrderId(), ex.getMessage());
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
