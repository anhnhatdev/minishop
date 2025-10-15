package com.minishop.paymentservice.event.consumer;

import com.minishop.paymentservice.event.dto.PaymentRequestedEvent;
import com.minishop.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestedEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "payment.requested", groupId = "payment-service-group")
    public void handlePaymentRequested(PaymentRequestedEvent event) {
        log.info("Received payment.requested for orderId: {}", event.getOrderId());
        paymentService.processPaymentRequested(event);
    }
}
