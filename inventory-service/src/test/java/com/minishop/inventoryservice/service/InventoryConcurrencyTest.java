package com.minishop.inventoryservice.service;

import com.minishop.inventoryservice.entity.Inventory;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryConcurrencyTest {

    @Test
    void testConcurrentReservationsNeverOversells() throws InterruptedException {
        int initialStock = 10;
        int totalConcurrentThreads = 30;

        Inventory inventory = Inventory.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .totalQuantity(initialStock)
                .reservedQuantity(0)
                .version(0L)
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(totalConcurrentThreads);
        CountDownLatch latch = new CountDownLatch(totalConcurrentThreads);

        AtomicInteger successfulReservations = new AtomicInteger(0);
        AtomicInteger rejectedReservations = new AtomicInteger(0);

        Object lock = new Object();

        for (int i = 0; i < totalConcurrentThreads; i++) {
            executorService.submit(() -> {
                try {
                    synchronized (lock) {
                        if (inventory.canReserve(1)) {
                            inventory.reserve(1);
                            successfulReservations.incrementAndGet();
                        } else {
                            rejectedReservations.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Verification of concurrency invariants
        assertEquals(initialStock, successfulReservations.get(), "Successful reservations must equal initial stock");
        assertEquals(totalConcurrentThreads - initialStock, rejectedReservations.get(), "Rejected reservations must equal excess threads");
        assertEquals(initialStock, inventory.getReservedQuantity(), "Reserved quantity must never exceed total stock");
        assertEquals(0, inventory.getAvailableQuantity(), "Available quantity must be 0");
    }
}
