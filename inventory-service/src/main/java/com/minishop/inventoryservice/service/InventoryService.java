package com.minishop.inventoryservice.service;

import com.minishop.inventoryservice.dto.request.AdjustStockRequest;
import com.minishop.inventoryservice.dto.request.ImportStockRequest;
import com.minishop.inventoryservice.dto.response.InventoryResponse;
import com.minishop.inventoryservice.dto.response.StockMovementResponse;
import com.minishop.inventoryservice.entity.*;
import com.minishop.inventoryservice.event.dto.*;
import com.minishop.inventoryservice.event.producer.InventoryEventProducer;
import com.minishop.inventoryservice.exception.InsufficientStockException;
import com.minishop.inventoryservice.exception.InventoryNotFoundException;
import com.minishop.inventoryservice.mapper.InventoryMapper;
import com.minishop.inventoryservice.repository.InventoryRepository;
import com.minishop.inventoryservice.repository.ProcessedEventRepository;
import com.minishop.inventoryservice.repository.StockMovementRepository;
import com.minishop.inventoryservice.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository stockReservationRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final InventoryEventProducer inventoryEventProducer;
    private final InventoryMapper inventoryMapper;

    /**
     * Optimistic Locking stock reservation with automatic retry on conflict.
     * All items within an order must be reserved atomically (all-or-nothing).
     */
    @Retryable(
            retryFor = {OptimisticLockingFailureException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public void processOrderCreated(OrderCreatedEvent event) {
        if (isAlreadyProcessed(event.getEventId())) {
            return;
        }

        log.info("Processing order.created reservation for orderId: {}", event.getOrderId());

        try {
            // Step 1: Pre-check availability for ALL items atomically
            for (OrderCreatedEvent.OrderItemEventDto item : event.getItems()) {
                Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                        .orElseThrow(() -> new InsufficientStockException("Inventory not found for product: " + item.getProductName()));

                if (!inventory.canReserve(item.getQuantity())) {
                    throw new InsufficientStockException(
                            "Product [" + item.getProductName() + "] is out of stock. Available: " + inventory.getAvailableQuantity() + ", requested: " + item.getQuantity()
                    );
                }
            }

            // Step 2: Perform reservations & audit logs
            for (OrderCreatedEvent.OrderItemEventDto item : event.getItems()) {
                Inventory inventory = inventoryRepository.findByProductId(item.getProductId()).get();
                inventory.reserve(item.getQuantity());
                inventoryRepository.save(inventory);

                StockReservation reservation = StockReservation.builder()
                        .orderId(event.getOrderId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .status(ReservationStatus.RESERVED)
                        .reservedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(15 * 60))
                        .build();
                stockReservationRepository.save(reservation);

                StockMovement movement = StockMovement.builder()
                        .productId(item.getProductId())
                        .type(StockMovementType.RESERVE)
                        .quantityChange(-item.getQuantity())
                        .referenceOrderId(event.getOrderId())
                        .note("Reserved for order " + event.getOrderCode())
                        .createdAt(Instant.now())
                        .build();
                stockMovementRepository.save(movement);
            }

            markAsProcessed(event.getEventId(), "order.created");

            // Publish success event
            StockReservedEvent reservedEvent = StockReservedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("stock.reserved")
                    .orderId(event.getOrderId())
                    .timestamp(Instant.now())
                    .build();
            inventoryEventProducer.publishStockReserved(reservedEvent);

        } catch (InsufficientStockException ex) {
            log.warn("Stock reservation failed for order {}: {}", event.getOrderId(), ex.getMessage());
            markAsProcessed(event.getEventId(), "order.created");

            StockRejectedEvent rejectedEvent = StockRejectedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("stock.rejected")
                    .orderId(event.getOrderId())
                    .reason(ex.getMessage())
                    .timestamp(Instant.now())
                    .build();
            inventoryEventProducer.publishStockRejected(rejectedEvent);
        }
    }

    @Transactional
    public void processOrderConfirmed(OrderConfirmedEvent event) {
        if (isAlreadyProcessed(event.getEventId())) {
            return;
        }

        log.info("Processing order.confirmed stock deduction for orderId: {}", event.getOrderId());
        List<StockReservation> reservations = stockReservationRepository.findByOrderIdAndStatus(event.getOrderId(), ReservationStatus.RESERVED);

        for (StockReservation reservation : reservations) {
            inventoryRepository.findByProductId(reservation.getProductId()).ifPresent(inventory -> {
                inventory.deduct(reservation.getQuantity());
                inventoryRepository.save(inventory);

                reservation.setStatus(ReservationStatus.CONFIRMED);
                stockReservationRepository.save(reservation);

                StockMovement movement = StockMovement.builder()
                        .productId(reservation.getProductId())
                        .type(StockMovementType.DEDUCT)
                        .quantityChange(-reservation.getQuantity())
                        .referenceOrderId(event.getOrderId())
                        .note("Stock deducted permanently on order confirmation")
                        .createdAt(Instant.now())
                        .build();
                stockMovementRepository.save(movement);

                // Reverse synchronization with product-service
                publishInventoryUpdate(inventory);
            });
        }

        markAsProcessed(event.getEventId(), "order.confirmed");
    }

    @Transactional
    public void processOrderCancelled(OrderCancelledEvent event) {
        if (isAlreadyProcessed(event.getEventId())) {
            return;
        }

        log.info("Processing order.cancelled stock release for orderId: {}", event.getOrderId());
        List<StockReservation> reservations = stockReservationRepository.findByOrderIdAndStatus(event.getOrderId(), ReservationStatus.RESERVED);

        for (StockReservation reservation : reservations) {
            inventoryRepository.findByProductId(reservation.getProductId()).ifPresent(inventory -> {
                inventory.release(reservation.getQuantity());
                inventoryRepository.save(inventory);

                reservation.setStatus(ReservationStatus.RELEASED);
                stockReservationRepository.save(reservation);

                StockMovement movement = StockMovement.builder()
                        .productId(reservation.getProductId())
                        .type(StockMovementType.RELEASE)
                        .quantityChange(reservation.getQuantity())
                        .referenceOrderId(event.getOrderId())
                        .note("Stock released due to order cancellation: " + event.getReason())
                        .createdAt(Instant.now())
                        .build();
                stockMovementRepository.save(movement);

                publishInventoryUpdate(inventory);
            });
        }

        markAsProcessed(event.getEventId(), "order.cancelled");
    }

    @Transactional
    public void releaseExpiredReservations() {
        List<StockReservation> expiredList = stockReservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.RESERVED, Instant.now()
        );

        if (!expiredList.isEmpty()) {
            log.warn("Found {} expired stock reservations. Releasing orphaned reservations...", expiredList.size());
            for (StockReservation reservation : expiredList) {
                inventoryRepository.findByProductId(reservation.getProductId()).ifPresent(inventory -> {
                    inventory.release(reservation.getQuantity());
                    inventoryRepository.save(inventory);

                    reservation.setStatus(ReservationStatus.RELEASED);
                    stockReservationRepository.save(reservation);

                    StockMovement movement = StockMovement.builder()
                            .productId(reservation.getProductId())
                            .type(StockMovementType.RELEASE)
                            .quantityChange(reservation.getQuantity())
                            .referenceOrderId(reservation.getOrderId())
                            .note("Auto-released orphan reservation due to 15-min timeout")
                            .createdAt(Instant.now())
                            .build();
                    stockMovementRepository.save(movement);

                    publishInventoryUpdate(inventory);
                });
            }
        }
    }

    @Transactional
    public InventoryResponse importStock(ImportStockRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                .orElseGet(() -> Inventory.builder()
                        .productId(request.getProductId())
                        .totalQuantity(0)
                        .reservedQuantity(0)
                        .build());

        inventory.importStock(request.getQuantity());
        Inventory saved = inventoryRepository.save(inventory);

        StockMovement movement = StockMovement.builder()
                .productId(request.getProductId())
                .type(StockMovementType.IMPORT)
                .quantityChange(request.getQuantity())
                .note(request.getNote() != null ? request.getNote() : "Stock import")
                .createdAt(Instant.now())
                .build();
        stockMovementRepository.save(movement);

        publishInventoryUpdate(saved);
        return inventoryMapper.toInventoryResponse(saved);
    }

    @Transactional
    public InventoryResponse adjustStock(UUID productId, AdjustStockRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));

        inventory.adjust(request.getQuantityChange());
        Inventory saved = inventoryRepository.save(inventory);

        StockMovement movement = StockMovement.builder()
                .productId(productId)
                .type(StockMovementType.ADJUST)
                .quantityChange(request.getQuantityChange())
                .note(request.getNote())
                .createdAt(Instant.now())
                .build();
        stockMovementRepository.save(movement);

        publishInventoryUpdate(saved);
        return inventoryMapper.toInventoryResponse(saved);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));
        return inventoryMapper.toInventoryResponse(inventory);
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getStockMovements(UUID productId) {
        List<StockMovement> movements = stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId);
        return inventoryMapper.toStockMovementResponseList(movements);
    }

    private void publishInventoryUpdate(Inventory inventory) {
        InventoryUpdatedEvent event = InventoryUpdatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("inventory.updated")
                .productId(inventory.getProductId())
                .availableQuantity(inventory.getAvailableQuantity())
                .timestamp(Instant.now())
                .build();
        inventoryEventProducer.publishInventoryUpdated(event);
    }

    private boolean isAlreadyProcessed(String eventId) {
        if (eventId == null) return false;
        boolean exists = processedEventRepository.existsById(eventId);
        if (exists) {
            log.warn("Event {} already processed in inventory-service, skipping duplicate", eventId);
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
