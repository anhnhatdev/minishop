package com.minishop.paymentservice.scheduler;

import com.minishop.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutScheduler {

    private final PaymentService paymentService;

    // Scan every 60 seconds for expired payment transactions
    @Scheduled(fixedRate = 60000)
    public void run() {
        paymentService.scanAndExpireStuckTransactions();
    }
}
