package com.minishop.notificationservice.scheduler;

import com.minishop.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryScheduler {

    private final NotificationService notificationService;

    // Retry failed notifications every 60 seconds (up to 3 attempts)
    @Scheduled(fixedRate = 60000)
    public void run() {
        notificationService.retryFailedNotifications();
    }
}
