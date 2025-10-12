package com.minishop.paymentservice.event.producer;

import com.minishop.paymentservice.event.dto.PaymentFailedEvent;
import com.minishop.paymentservice.event.dto.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String TOPIC_PAYMENT_SUCCEEDED = "payment.succeeded";
    public static final String TOPIC_PAYMENT_FAILED = "payment.failed";

    public void publishPaymentSucceeded(PaymentSucceededEvent event) {
        log.info("Publishing {} for orderId: {}, transactionId: {}", TOPIC_PAYMENT_SUCCEEDED, event.getOrderId(), event.getTransactionId());
        kafkaTemplate.send(TOPIC_PAYMENT_SUCCEEDED, event.getOrderId().toString(), event);
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.info("Publishing {} for orderId: {}, reason: {}", TOPIC_PAYMENT_FAILED, event.getOrderId(), event.getReason());
        kafkaTemplate.send(TOPIC_PAYMENT_FAILED, event.getOrderId().toString(), event);
    }
}
