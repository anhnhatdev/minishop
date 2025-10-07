package com.minishop.inventoryservice.event.consumer;

import com.minishop.inventoryservice.event.dto.OrderCancelledEvent;
import com.minishop.inventoryservice.event.dto.OrderConfirmedEvent;
import com.minishop.inventoryservice.event.dto.OrderCreatedEvent;
import com.minishop.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventsConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(topics = "order.created", groupId = "inventory-service-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order.created event for orderId: {}", event.getOrderId());
        inventoryService.processOrderCreated(event);
    }

    @KafkaListener(topics = "order.confirmed", groupId = "inventory-service-group")
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Received order.confirmed event for orderId: {}", event.getOrderId());
        inventoryService.processOrderConfirmed(event);
    }

    @KafkaListener(topics = "order.cancelled", groupId = "inventory-service-group")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received order.cancelled event for orderId: {}", event.getOrderId());
        inventoryService.processOrderCancelled(event);
    }
}
