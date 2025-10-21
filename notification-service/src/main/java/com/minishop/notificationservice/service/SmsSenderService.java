package com.minishop.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsSenderService {

    public void sendSms(String phoneNumber, String message) {
        log.info("[SMS GATEWAY MOCK] Dispatched SMS to '{}' | Content: '{}'", phoneNumber, message);
    }
}
