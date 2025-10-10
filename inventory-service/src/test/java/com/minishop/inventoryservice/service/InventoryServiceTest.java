package com.minishop.inventoryservice.service;

import com.minishop.inventoryservice.dto.request.AdjustStockRequest;
import com.minishop.inventoryservice.dto.request.ImportStockRequest;
import com.minishop.inventoryservice.dto.response.InventoryResponse;
import com.minishop.inventoryservice.entity.*;
import com.minishop.inventoryservice.event.dto.*;
import com.minishop.inventoryservice.event.producer.InventoryEventProducer;
import com.minishop.inventoryservice.mapper.InventoryMapper;
import com.minishop.inventoryservice.repository.InventoryRepository;
import com.minishop.inventoryservice.repository.ProcessedEventRepository;
import com.minishop.inventoryservice.repository.StockMovementRepository;
import com.minishop.inventoryservice.repository.StockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private InventoryEventProducer inventoryEventProducer;

    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID productId;
    private Inventory sampleInventory;
    private InventoryResponse sampleInventoryResponse;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();

        sampleInventory = Inventory.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .totalQuantity(50)
                .reservedQuantity(10)
                .version(1L)
                .build();

        sampleInventoryResponse = InventoryResponse.builder()
                .id(sampleInventory.getId())
                .productId(productId)
                .totalQuantity(50)
                .reservedQuantity(10)
                .availableQuantity(40)
                .version(1L)
                .build();
    }

    @Test
    void testImportStockSuccess() {
        ImportStockRequest request = ImportStockRequest.builder()
                .productId(productId)
                .quantity(30)
                .note("Import August batch")
                .build();

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(sampleInventory);
        when(inventoryMapper.toInventoryResponse(sampleInventory)).thenReturn(sampleInventoryResponse);

        InventoryResponse response = inventoryService.importStock(request);

        assertNotNull(response);
        assertEquals(80, sampleInventory.getTotalQuantity());
        verify(stockMovementRepository).save(any(StockMovement.class));
        verify(inventoryEventProducer).publishInventoryUpdated(any(InventoryUpdatedEvent.class));
    }

    @Test
    void testAdjustStockSuccess() {
        AdjustStockRequest request = AdjustStockRequest.builder()
                .quantityChange(-5)
                .note("Inventory audit correction")
                .build();

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(sampleInventory);
        when(inventoryMapper.toInventoryResponse(sampleInventory)).thenReturn(sampleInventoryResponse);

        InventoryResponse response = inventoryService.adjustStock(productId, request);

        assertNotNull(response);
        assertEquals(45, sampleInventory.getTotalQuantity());
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    void testProcessOrderCreatedSuccess() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID())
                .orderCode("ORD20260814001")
                .items(List.of(
                        OrderCreatedEvent.OrderItemEventDto.builder()
                                .productId(productId)
                                .productName("Áo thun")
                                .quantity(5)
                                .build()
                ))
                .build();

        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));

        inventoryService.processOrderCreated(event);

        assertEquals(15, sampleInventory.getReservedQuantity());
        verify(stockReservationRepository).save(any(StockReservation.class));
        verify(inventoryEventProducer).publishStockReserved(any(StockReservedEvent.class));
    }

    @Test
    void testProcessOrderCreatedInsufficientStockPublishesRejected() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID())
                .orderCode("ORD20260814002")
                .items(List.of(
                        OrderCreatedEvent.OrderItemEventDto.builder()
                                .productId(productId)
                                .productName("Áo thun")
                                .quantity(100) // Available is 40, so 100 exceeds stock
                                .build()
                ))
                .build();

        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));

        inventoryService.processOrderCreated(event);

        // reserved_quantity should remain unchanged
        assertEquals(10, sampleInventory.getReservedQuantity());
        verify(inventoryEventProducer).publishStockRejected(any(StockRejectedEvent.class));
        verify(stockReservationRepository, never()).save(any(StockReservation.class));
    }

    @Test
    void testProcessOrderConfirmedDeductsStock() {
        UUID orderId = UUID.randomUUID();
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .build();

        StockReservation reservation = StockReservation.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .productId(productId)
                .quantity(10)
                .status(ReservationStatus.RESERVED)
                .build();

        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                .thenReturn(List.of(reservation));
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));

        inventoryService.processOrderConfirmed(event);

        assertEquals(40, sampleInventory.getTotalQuantity());
        assertEquals(0, sampleInventory.getReservedQuantity());
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    void testProcessOrderCancelledReleasesStock() {
        UUID orderId = UUID.randomUUID();
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .reason("User cancelled")
                .build();

        StockReservation reservation = StockReservation.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .productId(productId)
                .quantity(10)
                .status(ReservationStatus.RESERVED)
                .build();

        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                .thenReturn(List.of(reservation));
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));

        inventoryService.processOrderCancelled(event);

        assertEquals(50, sampleInventory.getTotalQuantity()); // total unchanged
        assertEquals(0, sampleInventory.getReservedQuantity());  // reserved released
        assertEquals(ReservationStatus.RELEASED, reservation.getStatus());
        verify(stockMovementRepository).save(any(StockMovement.class));
    }
}
