package com.minishop.inventoryservice.event.producer;

import com.minishop.inventoryservice.event.dto.InventoryUpdatedEvent;
import com.minishop.inventoryservice.event.dto.StockRejectedEvent;
import com.minishop.inventoryservice.event.dto.StockReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String TOPIC_STOCK_RESERVED = "stock.reserved";
    public static final String TOPIC_STOCK_REJECTED = "stock.rejected";
    public static final String TOPIC_INVENTORY_UPDATED = "inventory.updated";

    public void publishStockReserved(StockReservedEvent event) {
        log.info("Publishing {} for orderId: {}", TOPIC_STOCK_RESERVED, event.getOrderId());
        kafkaTemplate.send(TOPIC_STOCK_RESERVED, event.getOrderId().toString(), event);
    }

    public void publishStockRejected(StockRejectedEvent event) {
        log.info("Publishing {} for orderId: {}, reason: {}", TOPIC_STOCK_REJECTED, event.getOrderId(), event.getReason());
        kafkaTemplate.send(TOPIC_STOCK_REJECTED, event.getOrderId().toString(), event);
    }

    public void publishInventoryUpdated(InventoryUpdatedEvent event) {
        log.info("Publishing {} for productId: {}, availableQuantity: {}", TOPIC_INVENTORY_UPDATED, event.getProductId(), event.getAvailableQuantity());
        kafkaTemplate.send(TOPIC_INVENTORY_UPDATED, event.getProductId().toString(), event);
    }
}
