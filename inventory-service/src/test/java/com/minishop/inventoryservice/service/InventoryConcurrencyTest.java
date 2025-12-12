package com.minishop.inventoryservice.service;

import com.minishop.inventoryservice.entity.Inventory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class InventoryConcurrencyTest {

    @Test
    @DisplayName("Verify anti-oversell invariant under high-concurrency contention")
    void testConcurrentReservationsNeverOversells() throws Exception {
        final int initialStock = 20;
        final int totalConcurrentRequests = 50;
        final UUID productId = UUID.randomUUID();

        // Simulated atomic in-memory repository representing PostgreSQL optimistic locking (@Version)
        ConcurrentHashMap<UUID, Inventory> dbStore = new ConcurrentHashMap<>();
        AtomicLong versionCounter = new AtomicLong(0);

        Inventory initialInventory = Inventory.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .totalQuantity(initialStock)
                .reservedQuantity(0)
                .version(versionCounter.get())
                .build();
        dbStore.put(productId, initialInventory);

        ExecutorService executorService = Executors.newFixedThreadPool(16);
        CountDownLatch readyLatch = new CountDownLatch(totalConcurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalConcurrentRequests);

        AtomicInteger successfulReservations = new AtomicInteger(0);
        AtomicInteger rejectedOutOfStock = new AtomicInteger(0);
        AtomicInteger optimisticLockRetries = new AtomicInteger(0);

        for (int i = 0; i < totalConcurrentRequests; i++) {
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Thundering herd: all threads start simultaneously

                    // Simulate Spring @Retryable (max 5 attempts) on Optimistic Lock Conflict
                    boolean committed = false;
                    for (int attempt = 0; attempt < 5 && !committed; attempt++) {
                        if (attempt > 0) {
                            optimisticLockRetries.incrementAndGet();
                            Thread.sleep(5); // jitter backoff
                        }

                        Inventory current = dbStore.get(productId);
                        if (!current.canReserve(1)) {
                            rejectedOutOfStock.incrementAndGet();
                            committed = true; // Legit out of stock, no retry needed
                            break;
                        }

                        // Prepare updated snapshot with incremented version
                        Inventory updated = Inventory.builder()
                                .id(current.getId())
                                .productId(current.getProductId())
                                .totalQuantity(current.getTotalQuantity())
                                .reservedQuantity(current.getReservedQuantity() + 1)
                                .version(current.getVersion() + 1)
                                .build();

                        // Atomic CAS: simulate PostgreSQL UPDATE inventory SET version=v+1, reserved=r+1 WHERE id=? AND version=v
                        if (dbStore.replace(productId, current, updated)) {
                            successfulReservations.incrementAndGet();
                            committed = true;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Release all threads at once
        assertTrue(finishLatch.await(10, TimeUnit.SECONDS), "All concurrent requests must complete within timeout");
        executorService.shutdown();

        // Strict validation of anti-oversell invariants
        Inventory finalInventory = dbStore.get(productId);
        assertNotNull(finalInventory);
        assertEquals(initialStock, successfulReservations.get(), "Total successful reservations MUST exactly match initial stock");
        assertEquals(totalConcurrentRequests - initialStock, rejectedOutOfStock.get(), "Rejected requests MUST equal total minus stock");
        assertEquals(initialStock, finalInventory.getReservedQuantity(), "Reserved quantity must equal initial stock");
        assertEquals(0, finalInventory.getAvailableQuantity(), "Available quantity must strictly reach 0 without going negative");
        assertTrue(finalInventory.getVersion() >= initialStock, "Version counter must reflect optimistic updates");
    }
}
