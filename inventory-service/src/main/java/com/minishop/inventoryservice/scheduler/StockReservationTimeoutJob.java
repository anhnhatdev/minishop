package com.minishop.inventoryservice.scheduler;

import com.minishop.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockReservationTimeoutJob {

    private final InventoryService inventoryService;

    // Scan every 60 seconds for expired stock reservations
    @Scheduled(fixedRate = 60000)
    public void run() {
        inventoryService.releaseExpiredReservations();
    }
}
